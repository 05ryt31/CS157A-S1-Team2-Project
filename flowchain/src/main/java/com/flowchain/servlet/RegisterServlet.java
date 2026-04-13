package com.flowchain.servlet;

import com.flowchain.db.DBConnection;
import com.flowchain.util.CsrfUtil;
import com.flowchain.util.PasswordUtil;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * GET  /register → render registration form
 * POST /register → validate, create org + location + user + orgmember + audit log
 *                  in a single SERIALIZABLE transaction, then log the user in
 *                  and redirect to the role-specific dashboard.
 *
 * SQL is inline (per CS 157A course style) and all queries use
 * PreparedStatement with ? placeholders to prevent SQL injection.
 *
 * IDs are generated via SELECT COALESCE(MAX(id), 0) + 1 because the
 * schema does not declare AUTO_INCREMENT. The SERIALIZABLE isolation
 * level prevents two concurrent registrations from picking the same ID.
 */
@WebServlet("/register")
public class RegisterServlet extends HttpServlet {

    private static final Pattern EMAIL_RE =
        Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private static final int MIN_PASSWORD_LEN = 8;
    private static final int MAX_RETRIES_ON_DEADLOCK = 3;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        String roleParam = req.getParameter("role");
        String role = ("recipient".equalsIgnoreCase(roleParam)) ? "RECIPIENT" : "DONOR";
        req.setAttribute("role", role);
        req.setAttribute("csrfToken", CsrfUtil.getOrCreate(req));
        req.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(req, res);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        if (!CsrfUtil.isValid(req)) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid CSRF token");
            return;
        }

        Form form = Form.from(req);
        Map<String, String> errors = form.validate();

        if (!errors.isEmpty()) {
            renderFormWithErrors(req, res, form, errors);
            return;
        }

        try {
            int newUserId = registerWithRetry(form);
            startSession(req, newUserId, form);
            res.sendRedirect(req.getContextPath() + dashboardPath(form.role));
        } catch (DuplicateException e) {
            errors.put(e.field, e.getMessage());
            renderFormWithErrors(req, res, form, errors);
        } catch (SQLException e) {
            getServletContext().log("Registration failed", e);
            errors.put("_global", "Could not complete registration. Please try again.");
            renderFormWithErrors(req, res, form, errors);
        }
    }

    /* ------------------------------------------------------------ */
    /*  Form parsing & validation                                   */
    /* ------------------------------------------------------------ */

    private static final class Form {
        String orgName;
        String orgType;     // free-text label like "Grocery Store" — defaults from role
        String phone;
        String address;
        String city;
        String zip;
        String fullName;
        String email;
        String password;
        String confirmPassword;
        String role;        // DONOR | RECIPIENT

        static Form from(HttpServletRequest req) {
            Form f = new Form();
            f.orgName         = trim(req.getParameter("orgName"));
            f.orgType         = trim(req.getParameter("orgType"));
            f.phone           = trim(req.getParameter("phone"));
            f.address         = trim(req.getParameter("address"));
            f.city            = trim(req.getParameter("city"));
            f.zip             = trim(req.getParameter("zip"));
            f.fullName        = trim(req.getParameter("fullName"));
            f.email           = trim(req.getParameter("email"));
            f.password        = req.getParameter("password");      // do NOT trim
            f.confirmPassword = req.getParameter("confirmPassword");
            String roleParam  = trim(req.getParameter("role"));
            f.role = "RECIPIENT".equalsIgnoreCase(roleParam) ? "RECIPIENT" : "DONOR";
            if (f.orgType == null || f.orgType.isEmpty()) {
                f.orgType = f.role.equals("DONOR") ? "Donor Organization" : "Recipient Organization";
            }
            return f;
        }

        Map<String, String> validate() {
            Map<String, String> errors = new LinkedHashMap<>();
            if (isBlank(orgName))  errors.put("orgName",  "Organization name is required.");
            if (isBlank(address))  errors.put("address",  "Address is required.");
            if (isBlank(city))     errors.put("city",     "City is required.");
            if (isBlank(zip))      errors.put("zip",      "ZIP is required.");
            if (isBlank(fullName)) errors.put("fullName", "Your name is required.");

            if (isBlank(email)) {
                errors.put("email", "Email is required.");
            } else if (!EMAIL_RE.matcher(email).matches()) {
                errors.put("email", "Email format is invalid.");
            }

            if (password == null || password.length() < MIN_PASSWORD_LEN) {
                errors.put("password",
                    "Password must be at least " + MIN_PASSWORD_LEN + " characters.");
            } else if (!password.equals(confirmPassword)) {
                errors.put("confirmPassword", "Passwords do not match.");
            }
            return errors;
        }

        private static String trim(String s) { return s == null ? null : s.trim(); }
        private static boolean isBlank(String s) { return s == null || s.isEmpty(); }
    }

    private void renderFormWithErrors(HttpServletRequest req, HttpServletResponse res,
                                      Form form, Map<String, String> errors)
            throws ServletException, IOException {
        req.setAttribute("form", form);
        req.setAttribute("errors", errors);
        req.setAttribute("role", form.role);
        req.setAttribute("csrfToken", CsrfUtil.getOrCreate(req));
        res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        req.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(req, res);
    }

    /* ------------------------------------------------------------ */
    /*  Database transaction                                        */
    /* ------------------------------------------------------------ */

    /** Thrown when org_name or email already exists. Carries the field name. */
    private static final class DuplicateException extends Exception {
        final String field;
        DuplicateException(String field, String message) { super(message); this.field = field; }
    }

    /**
     * Run the full registration transaction, retrying on deadlock (rare but
     * possible under SERIALIZABLE with MAX+1 ID generation).
     */
    private int registerWithRetry(Form form) throws SQLException, DuplicateException {
        SQLException lastError = null;
        for (int attempt = 1; attempt <= MAX_RETRIES_ON_DEADLOCK; attempt++) {
            try {
                return register(form);
            } catch (SQLException e) {
                // MySQL deadlock = SQLState 40001
                if ("40001".equals(e.getSQLState()) && attempt < MAX_RETRIES_ON_DEADLOCK) {
                    lastError = e;
                    continue;
                }
                throw e;
            }
        }
        throw lastError;
    }

    private int register(Form form) throws SQLException, DuplicateException {
        try (Connection conn = DBConnection.get()) {
            conn.setAutoCommit(false);
            conn.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            try {
                if (orgNameExists(conn, form.orgName)) {
                    throw new DuplicateException("orgName",
                        "An organization with this name is already registered.");
                }
                if (emailExists(conn, form.email)) {
                    throw new DuplicateException("email", "Email is already in use.");
                }

                int orgId   = nextId(conn, "organizations", "org_id");
                insertOrganization(conn, orgId, form);

                int locationId = nextId(conn, "location", "location_id");
                insertLocation(conn, locationId, orgId, form);

                int userId  = nextId(conn, "users", "user_id");
                insertUser(conn, userId, form);

                insertOrgMember(conn, orgId, userId);

                int logId = nextId(conn, "auditlogs", "log_id");
                insertAuditLog(conn, logId, userId, "REGISTER", "USER", userId);

                conn.commit();
                return userId;
            } catch (SQLException | DuplicateException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    private static int nextId(Connection conn, String table, String pkColumn) throws SQLException {
        // table and pkColumn are hard-coded constants from this file, never user input
        String sql = "SELECT COALESCE(MAX(" + pkColumn + "), 0) + 1 FROM " + table;
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private static boolean orgNameExists(Connection conn, String orgName) throws SQLException {
        String sql = "SELECT 1 FROM organizations WHERE org_name = ? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, orgName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static boolean emailExists(Connection conn, String email) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE email = ? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static void insertOrganization(Connection conn, int orgId, Form form) throws SQLException {
        String sql = "INSERT INTO organizations (org_id, org_name, org_type, phone, status) "
                   + "VALUES (?, ?, ?, ?, 'PENDING')";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt   (1, orgId);
            ps.setString(2, form.orgName);
            ps.setString(3, form.orgType);
            ps.setString(4, form.phone);
            ps.executeUpdate();
        }
    }

    private static void insertLocation(Connection conn, int locationId, int orgId, Form form)
            throws SQLException {
        String sql = "INSERT INTO location (location_id, org_id, address, city, zip) "
                   + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt   (1, locationId);
            ps.setInt   (2, orgId);
            ps.setString(3, form.address);
            ps.setString(4, form.city);
            ps.setString(5, form.zip);
            ps.executeUpdate();
        }
    }

    private static void insertUser(Connection conn, int userId, Form form) throws SQLException {
        String sql = "INSERT INTO users (user_id, name, email, password, role, created_at) "
                   + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt      (1, userId);
            ps.setString   (2, form.fullName);
            ps.setString   (3, form.email);
            ps.setString   (4, PasswordUtil.hash(form.password));
            ps.setString   (5, form.role);
            ps.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        }
    }

    private static void insertOrgMember(Connection conn, int orgId, int userId) throws SQLException {
        String sql = "INSERT INTO orgmembers (org_id, user_id, member_role) VALUES (?, ?, 'OWNER')";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orgId);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    private static void insertAuditLog(Connection conn, int logId, int userId,
                                       String actionType, String entityType, int entityId)
            throws SQLException {
        String sql = "INSERT INTO auditlogs (log_id, user_id, action_type, entity_type, "
                   + "entity_id, action_time) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt      (1, logId);
            ps.setInt      (2, userId);
            ps.setString   (3, actionType);
            ps.setString   (4, entityType);
            ps.setInt      (5, entityId);
            ps.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        }
    }

    /* ------------------------------------------------------------ */
    /*  Session bootstrap                                           */
    /* ------------------------------------------------------------ */

    private static void startSession(HttpServletRequest req, int userId, Form form) {
        // Session-fixation defense: discard any pre-existing (anonymous) session
        HttpSession existing = req.getSession(false);
        if (existing != null) existing.invalidate();

        HttpSession session = req.getSession(true);
        session.setAttribute("userId",   userId);
        session.setAttribute("role",     form.role);
        session.setAttribute("fullName", form.fullName);
        session.setAttribute("email",    form.email);
    }

    private static String dashboardPath(String role) {
        switch (role) {
            case "RECIPIENT": return "/recipient/dashboard";
            case "ADMIN":     return "/admin/dashboard";
            case "DONOR":
            default:          return "/donor/dashboard";
        }
    }
}

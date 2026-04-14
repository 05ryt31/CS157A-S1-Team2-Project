package com.flowchain.servlet;

import com.flowchain.db.DBConnection;
import com.flowchain.util.CsrfUtil;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Organization profile view and edit.
 *
 *   /donor/profile     — AuthFilter enforces DONOR; edits own org
 *   /recipient/profile — AuthFilter enforces RECIPIENT; edits own org
 *   /admin/org?id=X    — AuthFilter enforces ADMIN; edits any org
 *
 * GET  → load org + primary location + members → render JSP
 * POST → validate → UPDATE organizations + location + audit log → redirect
 */
@WebServlet(urlPatterns = {"/donor/profile", "/recipient/profile", "/admin/org"})
public class OrgProfileServlet extends HttpServlet {

    /* ------------------------------------------------------------ */
    /*  Data holders (accessible from JSP via getters)              */
    /* ------------------------------------------------------------ */

    public static final class OrgData {
        private int orgId;
        private String orgName;
        private String orgType;
        private String phone;
        private String status;

        public int getOrgId()       { return orgId; }
        public String getOrgName()  { return orgName; }
        public String getOrgType()  { return orgType; }
        public String getPhone()    { return phone; }
        public String getStatus()   { return status; }
    }

    public static final class LocationData {
        private int locationId;
        private String address;
        private String city;
        private String zip;

        public int getLocationId()  { return locationId; }
        public String getAddress()  { return address; }
        public String getCity()     { return city; }
        public String getZip()      { return zip; }
    }

    public static final class MemberRow {
        private String name;
        private String email;
        private String memberRole;

        public String getName()       { return name; }
        public String getEmail()      { return email; }
        public String getMemberRole() { return memberRole; }
    }

    /* ------------------------------------------------------------ */
    /*  GET                                                         */
    /* ------------------------------------------------------------ */

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        Integer orgId = resolveOrgId(req, res);
        if (orgId == null) return; // response already committed

        try {
            OrgData org = loadOrg(orgId);
            if (org == null) {
                res.sendError(HttpServletResponse.SC_NOT_FOUND, "Organization not found.");
                return;
            }

            LocationData location = loadPrimaryLocation(orgId);
            List<MemberRow> members = loadMembers(orgId);

            String userEmail = (String) req.getSession().getAttribute("email");
            boolean isAdmin = "ADMIN".equals(req.getSession().getAttribute("role"));

            req.setAttribute("org", org);
            req.setAttribute("location", location);
            req.setAttribute("members", members);
            req.setAttribute("userEmail", userEmail);
            req.setAttribute("isAdmin", isAdmin);
            req.setAttribute("csrfToken", CsrfUtil.getOrCreate(req));
            req.getRequestDispatcher("/WEB-INF/views/org-profile.jsp").forward(req, res);
        } catch (SQLException e) {
            getServletContext().log("Failed to load org profile", e);
            res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    /* ------------------------------------------------------------ */
    /*  POST                                                        */
    /* ------------------------------------------------------------ */

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        if (!CsrfUtil.isValid(req)) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid CSRF token");
            return;
        }

        Integer orgId = resolveOrgId(req, res);
        if (orgId == null) return;

        String orgName = trim(req.getParameter("orgName"));
        String orgType = trim(req.getParameter("orgType"));
        String phone   = trim(req.getParameter("phone"));
        String address = trim(req.getParameter("address"));
        String city    = trim(req.getParameter("city"));
        String zip     = trim(req.getParameter("zip"));
        String locationIdParam = req.getParameter("locationId");

        Map<String, String> errors = new LinkedHashMap<>();
        if (isBlank(orgName))  errors.put("orgName",  "Organization name is required.");
        if (isBlank(address))  errors.put("address",  "Address is required.");
        if (isBlank(city))     errors.put("city",     "City is required.");
        if (isBlank(zip))      errors.put("zip",      "ZIP is required.");

        if (!errors.isEmpty()) {
            reloadAndRenderErrors(req, res, orgId, errors);
            return;
        }

        try (Connection conn = DBConnection.get()) {
            conn.setAutoCommit(false);
            try {
                // Check org_name uniqueness (excluding this org)
                if (orgNameTakenByOther(conn, orgName, orgId)) {
                    errors.put("orgName", "An organization with this name already exists.");
                    conn.rollback();
                    reloadAndRenderErrors(req, res, orgId, errors);
                    return;
                }

                updateOrganization(conn, orgId, orgName, orgType, phone);

                if (locationIdParam != null && !locationIdParam.isEmpty()) {
                    int locationId = Integer.parseInt(locationIdParam);
                    updateLocation(conn, locationId, address, city, zip);
                } else {
                    // Org has no location yet — insert one
                    int locationId = nextId(conn, "location", "location_id");
                    insertLocation(conn, locationId, orgId, address, city, zip);
                }

                int userId = (Integer) req.getSession().getAttribute("userId");
                int logId = nextId(conn, "auditlogs", "log_id");
                insertAuditLog(conn, logId, userId, "UPDATE", "ORGANIZATION", orgId);

                conn.commit();

                // Update session fullName/orgName if user edited their own org
                updateSessionIfOwn(req, orgId);

                String redirectUrl = profileUrlForRole(req);
                res.sendRedirect(redirectUrl + "?saved=1");
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            getServletContext().log("Failed to update org profile", e);
            errors.put("_global", "Could not save changes. Please try again.");
            reloadAndRenderErrors(req, res, orgId, errors);
        }
    }

    /* ------------------------------------------------------------ */
    /*  Resolve which org to edit                                   */
    /* ------------------------------------------------------------ */

    private Integer resolveOrgId(HttpServletRequest req, HttpServletResponse res)
            throws IOException {
        HttpSession session = req.getSession(false);
        if (session == null) {
            res.sendRedirect(req.getContextPath() + "/login");
            return null;
        }

        String role = (String) session.getAttribute("role");

        // Admin can edit any org via ?id=X
        if ("ADMIN".equals(role)) {
            String idParam = req.getParameter("id");
            if (idParam != null && !idParam.isEmpty()) {
                try {
                    return Integer.parseInt(idParam);
                } catch (NumberFormatException e) {
                    res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid org id.");
                    return null;
                }
            }
        }

        // Everyone else (or admin without ?id=) edits their own org
        Object orgIdAttr = session.getAttribute("orgId");
        if (orgIdAttr instanceof Integer) {
            return (Integer) orgIdAttr;
        }

        res.sendError(HttpServletResponse.SC_BAD_REQUEST,
            "No organization linked to your account.");
        return null;
    }

    /* ------------------------------------------------------------ */
    /*  Database reads                                              */
    /* ------------------------------------------------------------ */

    private static OrgData loadOrg(int orgId) throws SQLException {
        String sql = "SELECT org_id, org_name, org_type, phone, status "
                   + "FROM organizations WHERE org_id = ?";
        try (Connection conn = DBConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orgId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                OrgData o = new OrgData();
                o.orgId   = rs.getInt("org_id");
                o.orgName = rs.getString("org_name");
                o.orgType = rs.getString("org_type");
                o.phone   = rs.getString("phone");
                o.status  = rs.getString("status");
                return o;
            }
        }
    }

    private static LocationData loadPrimaryLocation(int orgId) throws SQLException {
        String sql = "SELECT location_id, address, city, zip "
                   + "FROM location WHERE org_id = ? ORDER BY location_id LIMIT 1";
        try (Connection conn = DBConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orgId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                LocationData l = new LocationData();
                l.locationId = rs.getInt("location_id");
                l.address    = rs.getString("address");
                l.city       = rs.getString("city");
                l.zip        = rs.getString("zip");
                return l;
            }
        }
    }

    private static List<MemberRow> loadMembers(int orgId) throws SQLException {
        String sql = "SELECT u.name, u.email, om.member_role "
                   + "FROM orgmembers om "
                   + "JOIN users u ON om.user_id = u.user_id "
                   + "WHERE om.org_id = ? "
                   + "ORDER BY om.member_role, u.name";
        List<MemberRow> rows = new ArrayList<>();
        try (Connection conn = DBConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orgId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    MemberRow m = new MemberRow();
                    m.name       = rs.getString("name");
                    m.email      = rs.getString("email");
                    m.memberRole = rs.getString("member_role");
                    rows.add(m);
                }
            }
        }
        return rows;
    }

    /* ------------------------------------------------------------ */
    /*  Database writes                                             */
    /* ------------------------------------------------------------ */

    private static boolean orgNameTakenByOther(Connection conn, String orgName, int excludeOrgId)
            throws SQLException {
        String sql = "SELECT 1 FROM organizations WHERE org_name = ? AND org_id != ? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, orgName);
            ps.setInt(2, excludeOrgId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static void updateOrganization(Connection conn, int orgId,
                                           String orgName, String orgType, String phone)
            throws SQLException {
        String sql = "UPDATE organizations SET org_name = ?, org_type = ?, phone = ? "
                   + "WHERE org_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, orgName);
            ps.setString(2, orgType);
            ps.setString(3, phone);
            ps.setInt(4, orgId);
            ps.executeUpdate();
        }
    }

    private static void updateLocation(Connection conn, int locationId,
                                       String address, String city, String zip)
            throws SQLException {
        String sql = "UPDATE location SET address = ?, city = ?, zip = ? "
                   + "WHERE location_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, address);
            ps.setString(2, city);
            ps.setString(3, zip);
            ps.setInt(4, locationId);
            ps.executeUpdate();
        }
    }

    private static void insertLocation(Connection conn, int locationId, int orgId,
                                       String address, String city, String zip)
            throws SQLException {
        String sql = "INSERT INTO location (location_id, org_id, address, city, zip) "
                   + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, locationId);
            ps.setInt(2, orgId);
            ps.setString(3, address);
            ps.setString(4, city);
            ps.setString(5, zip);
            ps.executeUpdate();
        }
    }

    private static int nextId(Connection conn, String table, String pkColumn) throws SQLException {
        String sql = "SELECT COALESCE(MAX(" + pkColumn + "), 0) + 1 FROM " + table;
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private static void insertAuditLog(Connection conn, int logId, int userId,
                                       String actionType, String entityType, int entityId)
            throws SQLException {
        String sql = "INSERT INTO auditlogs (log_id, user_id, action_type, entity_type, "
                   + "entity_id, action_time) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, logId);
            ps.setInt(2, userId);
            ps.setString(3, actionType);
            ps.setString(4, entityType);
            ps.setInt(5, entityId);
            ps.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        }
    }

    /* ------------------------------------------------------------ */
    /*  Helpers                                                     */
    /* ------------------------------------------------------------ */

    private void reloadAndRenderErrors(HttpServletRequest req, HttpServletResponse res,
                                       int orgId, Map<String, String> errors)
            throws ServletException, IOException {
        try {
            OrgData org = loadOrg(orgId);
            LocationData location = loadPrimaryLocation(orgId);
            List<MemberRow> members = loadMembers(orgId);

            req.setAttribute("org", org);
            req.setAttribute("location", location);
            req.setAttribute("members", members);
            req.setAttribute("errors", errors);
            req.setAttribute("userEmail", req.getSession().getAttribute("email"));
            req.setAttribute("isAdmin", "ADMIN".equals(req.getSession().getAttribute("role")));
            req.setAttribute("csrfToken", CsrfUtil.getOrCreate(req));
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            req.getRequestDispatcher("/WEB-INF/views/org-profile.jsp").forward(req, res);
        } catch (SQLException e) {
            getServletContext().log("Failed to reload org for error render", e);
            res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private static void updateSessionIfOwn(HttpServletRequest req, int editedOrgId) {
        HttpSession session = req.getSession(false);
        if (session == null) return;
        Object sessionOrgId = session.getAttribute("orgId");
        if (sessionOrgId instanceof Integer && (Integer) sessionOrgId == editedOrgId) {
            // Session org matches edited org — nothing to update for now
            // (fullName is on users, not org; org_name isn't stored in session)
        }
    }

    private String profileUrlForRole(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        String role = (session != null) ? (String) session.getAttribute("role") : null;
        String ctx = req.getContextPath();
        if ("ADMIN".equals(role)) {
            String idParam = req.getParameter("id");
            return ctx + "/admin/org" + (idParam != null ? "?id=" + idParam : "");
        }
        if ("RECIPIENT".equals(role)) return ctx + "/recipient/profile";
        return ctx + "/donor/profile";
    }

    private static String trim(String s) { return s == null ? null : s.trim(); }
    private static boolean isBlank(String s) { return s == null || s.isEmpty(); }
}

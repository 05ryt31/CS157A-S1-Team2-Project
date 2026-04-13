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
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * GET  /login → render login form (with optional ?next= for post-login redirect)
 * POST /login → verify credentials, regenerate session, redirect by role
 *
 * On any failure (unknown email OR wrong password) we return the same
 * "Invalid email or password" message — never reveal which field was
 * wrong (prevents user enumeration).
 */
@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        // If already logged in, send straight to their dashboard
        HttpSession existing = req.getSession(false);
        if (existing != null && existing.getAttribute("userId") != null) {
            String role = (String) existing.getAttribute("role");
            res.sendRedirect(req.getContextPath() + dashboardPath(role));
            return;
        }

        req.setAttribute("csrfToken", CsrfUtil.getOrCreate(req));
        req.setAttribute("next", req.getParameter("next"));
        req.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(req, res);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        if (!CsrfUtil.isValid(req)) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid CSRF token");
            return;
        }

        String email    = trim(req.getParameter("email"));
        String password = req.getParameter("password");
        String next     = req.getParameter("next");

        if (isBlank(email) || isBlank(password)) {
            renderError(req, res, email, next, "Invalid email or password.");
            return;
        }

        UserRow user;
        try {
            user = findByEmail(email);
        } catch (SQLException e) {
            getServletContext().log("Login lookup failed", e);
            renderError(req, res, email, next, "Could not log you in. Please try again.");
            return;
        }

        if (user == null || !PasswordUtil.verify(password, user.passwordHash)) {
            renderError(req, res, email, next, "Invalid email or password.");
            return;
        }

        // Best-effort: load primary org id (first orgmembers row), nullable
        Integer orgId = null;
        try {
            orgId = findPrimaryOrgId(user.userId);
        } catch (SQLException e) {
            getServletContext().log("Could not load org for user " + user.userId, e);
        }

        startSession(req, user, orgId);

        try {
            writeAuditLog(user.userId, "LOGIN", "USER", user.userId);
        } catch (SQLException e) {
            getServletContext().log("Audit log failed for LOGIN", e);
            // Login still succeeds — audit failure must not block sign-in
        }

        String redirectTo = safeNext(req, next);
        if (redirectTo == null) {
            redirectTo = req.getContextPath() + dashboardPath(user.role);
        }
        res.sendRedirect(redirectTo);
    }

    /* ------------------------------------------------------------ */

    private static final class UserRow {
        int userId;
        String name;
        String email;
        String passwordHash;
        String role;
    }

    private static UserRow findByEmail(String email) throws SQLException {
        String sql = "SELECT user_id, name, email, password, role FROM users WHERE email = ?";
        try (Connection conn = DBConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                UserRow u = new UserRow();
                u.userId       = rs.getInt   ("user_id");
                u.name         = rs.getString("name");
                u.email        = rs.getString("email");
                u.passwordHash = rs.getString("password");
                u.role         = rs.getString("role");
                return u;
            }
        }
    }

    private static Integer findPrimaryOrgId(int userId) throws SQLException {
        String sql = "SELECT org_id FROM orgmembers WHERE user_id = ? ORDER BY org_id LIMIT 1";
        try (Connection conn = DBConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : null;
            }
        }
    }

    private static void writeAuditLog(int userId, String actionType, String entityType, int entityId)
            throws SQLException {
        try (Connection conn = DBConnection.get()) {
            conn.setAutoCommit(false);
            try {
                int logId;
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT COALESCE(MAX(log_id), 0) + 1 FROM auditlogs");
                     ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    logId = rs.getInt(1);
                }
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO auditlogs (log_id, user_id, action_type, entity_type, "
                      + "entity_id, action_time) VALUES (?, ?, ?, ?, ?, ?)")) {
                    ps.setInt      (1, logId);
                    ps.setInt      (2, userId);
                    ps.setString   (3, actionType);
                    ps.setString   (4, entityType);
                    ps.setInt      (5, entityId);
                    ps.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
                    ps.executeUpdate();
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /* ------------------------------------------------------------ */

    private static void startSession(HttpServletRequest req, UserRow user, Integer orgId) {
        // Session-fixation defense: discard any pre-existing session
        HttpSession existing = req.getSession(false);
        if (existing != null) existing.invalidate();

        HttpSession session = req.getSession(true);
        session.setAttribute("userId",   user.userId);
        session.setAttribute("role",     user.role);
        session.setAttribute("fullName", user.name);
        session.setAttribute("email",    user.email);
        if (orgId != null) {
            session.setAttribute("orgId", orgId);
        }
    }

    /**
     * Only honour ?next= if it's an internal path (starts with this app's
     * context path and a slash). Prevents open-redirect attacks like
     * /login?next=https://evil.example.com.
     */
    private static String safeNext(HttpServletRequest req, String next) {
        if (next == null || next.isEmpty()) return null;
        String ctx = req.getContextPath();
        String prefix = ctx.isEmpty() ? "/" : ctx + "/";
        return next.startsWith(prefix) ? next : null;
    }

    private void renderError(HttpServletRequest req, HttpServletResponse res,
                             String email, String next, String message)
            throws ServletException, IOException {
        req.setAttribute("csrfToken", CsrfUtil.getOrCreate(req));
        req.setAttribute("error", message);
        req.setAttribute("email", email);
        req.setAttribute("next",  next);
        res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        req.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(req, res);
    }

    private static String dashboardPath(String role) {
        if (role == null) return "/";
        switch (role) {
            case "RECIPIENT": return "/recipient/dashboard";
            case "ADMIN":     return "/admin/dashboard";
            case "DONOR":     return "/donor/dashboard";
            default:          return "/";
        }
    }

    private static String trim(String s) { return s == null ? null : s.trim(); }
    private static boolean isBlank(String s) { return s == null || s.isEmpty(); }
}

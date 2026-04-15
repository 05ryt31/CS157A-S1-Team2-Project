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
 * POST /account/password
 *
 * Validates the current password, checks the new password meets our length
 * requirement and matches its confirmation, then UPDATEs users.password and
 * writes a PASSWORD_CHANGE audit log entry.
 *
 * Session stays active — user is not forced to log back in.
 */
@WebServlet("/account/password")
public class PasswordChangeServlet extends HttpServlet {

    private static final int MIN_PASSWORD_LEN = 8;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        if (!CsrfUtil.isValid(req)) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid CSRF token");
            return;
        }

        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            res.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        int userId = (Integer) session.getAttribute("userId");

        String currentPassword     = req.getParameter("currentPassword");
        String newPassword         = req.getParameter("newPassword");
        String confirmNewPassword  = req.getParameter("confirmNewPassword");

        // Validate input
        if (isBlank(currentPassword) || isBlank(newPassword) || isBlank(confirmNewPassword)) {
            renderError(req, res, "All password fields are required.");
            return;
        }
        if (newPassword.length() < MIN_PASSWORD_LEN) {
            renderError(req, res,
                "New password must be at least " + MIN_PASSWORD_LEN + " characters.");
            return;
        }
        if (!newPassword.equals(confirmNewPassword)) {
            renderError(req, res, "New passwords do not match.");
            return;
        }
        if (newPassword.equals(currentPassword)) {
            renderError(req, res, "New password must be different from the current password.");
            return;
        }

        try {
            String storedHash = findPasswordHash(userId);
            if (storedHash == null || !PasswordUtil.verify(currentPassword, storedHash)) {
                renderError(req, res, "Current password is incorrect.");
                return;
            }

            String newHash = PasswordUtil.hash(newPassword);
            updatePasswordAndAudit(userId, newHash);

            // Stay on the account page with a success message
            req.setAttribute("successMessage", "Password updated successfully.");
            req.setAttribute("csrfToken", CsrfUtil.getOrCreate(req));
            req.getRequestDispatcher("/WEB-INF/views/account.jsp").forward(req, res);

        } catch (SQLException e) {
            getServletContext().log("Password change failed for user " + userId, e);
            renderError(req, res, "Could not update password. Please try again.");
        }
    }

    /* ------------------------------------------------------------ */

    private static String findPasswordHash(int userId) throws SQLException {
        String sql = "SELECT password FROM users WHERE user_id = ?";
        try (Connection conn = DBConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("password") : null;
            }
        }
    }

    private static void updatePasswordAndAudit(int userId, String newHash) throws SQLException {
        try (Connection conn = DBConnection.get()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE users SET password = ? WHERE user_id = ?")) {
                    ps.setString(1, newHash);
                    ps.setInt(2, userId);
                    ps.executeUpdate();
                }

                int logId;
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT COALESCE(MAX(log_id), 0) + 1 FROM auditlogs");
                     ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    logId = rs.getInt(1);
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO auditlogs (log_id, user_id, action_type, entity_type, "
                      + "entity_id, action_time) VALUES (?, ?, 'PASSWORD_CHANGE', 'USER', ?, ?)")) {
                    ps.setInt      (1, logId);
                    ps.setInt      (2, userId);
                    ps.setInt      (3, userId);
                    ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
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

    private void renderError(HttpServletRequest req, HttpServletResponse res, String message)
            throws ServletException, IOException {
        req.setAttribute("passwordError", message);
        req.setAttribute("csrfToken", CsrfUtil.getOrCreate(req));
        res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        req.getRequestDispatcher("/WEB-INF/views/account.jsp").forward(req, res);
    }

    private static boolean isBlank(String s) { return s == null || s.isEmpty(); }
}

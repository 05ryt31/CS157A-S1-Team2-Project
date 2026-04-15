package com.flowchain.servlet;

import com.flowchain.db.DBConnection;
import com.flowchain.util.CsrfUtil;
import com.flowchain.util.PasswordUtil;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * POST /account/delete
 *
 * Deletes the currently logged-in user's account after verifying their
 * password.  DELETE FROM users cascades to orgmembers and auditlogs, so
 * all of this user's historical audit trail is wiped along with the user
 * row.  No ACCOUNT_DELETE audit log is written because the FK cascade
 * would immediately erase it too — recording the deletion in a way that
 * survives would require a schema change (e.g. SET NULL on the FK, or a
 * separate deletion-events table).
 *
 * The organization the user belonged to is intentionally NOT deleted,
 * even if this user was its only member.  Orphaned orgs can be cleaned
 * up by an admin later.
 *
 * On success: session invalidated, redirect to "/".
 */
@WebServlet("/account/delete")
public class AccountDeleteServlet extends HttpServlet {

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
        String confirmPassword = req.getParameter("confirmPassword");

        if (confirmPassword == null || confirmPassword.isEmpty()) {
            renderError(req, res, "Please enter your password to confirm deletion.");
            return;
        }

        try {
            String storedHash = findPasswordHash(userId);
            if (storedHash == null || !PasswordUtil.verify(confirmPassword, storedHash)) {
                renderError(req, res, "Password is incorrect. Account not deleted.");
                return;
            }

            deleteUser(userId);
            session.invalidate();
            res.sendRedirect(req.getContextPath() + "/?deleted=1");

        } catch (SQLException e) {
            getServletContext().log("Account deletion failed for user " + userId, e);
            renderError(req, res, "Could not delete account. Please try again.");
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

    private static void deleteUser(int userId) throws SQLException {
        String sql = "DELETE FROM users WHERE user_id = ?";
        try (Connection conn = DBConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    /* ------------------------------------------------------------ */

    private void renderError(HttpServletRequest req, HttpServletResponse res, String message)
            throws ServletException, IOException {
        req.setAttribute("deleteError", message);
        req.setAttribute("csrfToken", CsrfUtil.getOrCreate(req));
        res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        req.getRequestDispatcher("/WEB-INF/views/account.jsp").forward(req, res);
    }
}

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
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * POST /logout → write a LOGOUT audit log row, invalidate the session,
 *                redirect to "/".
 *
 * GET is intentionally not supported: logging out via a link a third
 * party can embed (CSRF) would let attackers force-logout users.
 */
@WebServlet("/logout")
public class LogoutServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        if (!CsrfUtil.isValid(req)) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid CSRF token");
            return;
        }

        HttpSession session = req.getSession(false);
        if (session != null) {
            Object userIdAttr = session.getAttribute("userId");
            if (userIdAttr instanceof Integer) {
                try {
                    writeAuditLog((Integer) userIdAttr);
                } catch (SQLException e) {
                    getServletContext().log("Audit log failed for LOGOUT", e);
                    // Logout still proceeds — audit failure must not block sign-out
                }
            }
            session.invalidate();
        }

        res.sendRedirect(req.getContextPath() + "/");
    }

    private static void writeAuditLog(int userId) throws SQLException {
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
                      + "entity_id, action_time) VALUES (?, ?, 'LOGOUT', 'USER', ?, ?)")) {
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
}

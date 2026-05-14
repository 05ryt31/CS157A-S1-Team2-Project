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

@WebServlet("/admin/claim/action")
public class AdminClaimActionServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        if (!CsrfUtil.isValid(req)) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid CSRF token");
            return;
        }

        HttpSession session = req.getSession(false);
        String role = getSessionRole(session);
        Integer userId = getSessionUserId(session);

        if (!"ADMIN".equals(role) || userId == null) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        Integer claimId = parseInt(req.getParameter("claimId"));
        String action = trim(req.getParameter("action"));

        if (claimId == null || action == null) {
            res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing claim id or action.");
            return;
        }

        try {
            if ("approve".equals(action)) {
                applyAction(userId, claimId, "APPROVED");
                setFlash(session, "success", "Claim approved.");
            } else if ("reject".equals(action)) {
                applyAction(userId, claimId, "REJECTED");
                setFlash(session, "success", "Claim rejected.");
            } else {
                res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknown action.");
                return;
            }
            res.sendRedirect(req.getContextPath() + "/admin/claims");
        } catch (SQLException e) {
            getServletContext().log("Admin claim action failed", e);
            setFlash(session, "error", "Could not update claim.");
            res.sendRedirect(req.getContextPath() + "/admin/claims");
        }
    }

    static void applyAction(int adminUserId, int claimId, String newStatus) throws SQLException {
        try (Connection conn = DBConnection.get()) {
            conn.setAutoCommit(false);

            try {
                int listingId;
                String currentStatus;

                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT listing_id, status FROM claims WHERE claim_id = ? FOR UPDATE")) {
                    ps.setInt(1, claimId);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            return;
                        }
                        listingId = rs.getInt("listing_id");
                        currentStatus = rs.getString("status");
                    }
                }

                // idempotent: only PENDING claims can transition
                if (!"PENDING".equals(currentStatus)) {
                    conn.rollback();
                    return;
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE claims SET status = ? WHERE claim_id = ?")) {
                    ps.setString(1, newStatus);
                    ps.setInt(2, claimId);
                    ps.executeUpdate();
                }

                // on approve, lock the listing so others can't claim it
                if ("APPROVED".equals(newStatus)) {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "UPDATE listings SET status = 'CLAIMED' WHERE listing_id = ?")) {
                        ps.setInt(1, listingId);
                        ps.executeUpdate();
                    }
                }

                String auditAction = "APPROVED".equals(newStatus) ? "APPROVE_CLAIM" : "REJECT_CLAIM";
                writeAuditLog(conn, adminUserId, auditAction, "CLAIM", claimId);

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    private static int nextId(Connection conn, String table, String idColumn) throws SQLException {
        String sql = "SELECT COALESCE(MAX(" + idColumn + "), 0) + 1 FROM " + table;
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private static void writeAuditLog(Connection conn, int userId, String actionType,
                                      String entityType, int entityId) throws SQLException {
        int logId = nextId(conn, "auditlogs", "log_id");

        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO auditlogs (log_id, user_id, action_type, entity_type, entity_id, action_time) "
              + "VALUES (?, ?, ?, ?, ?, ?)")) {
            ps.setInt(1, logId);
            ps.setInt(2, userId);
            ps.setString(3, actionType);
            ps.setString(4, entityType);
            ps.setInt(5, entityId);
            ps.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        }
    }

    private static void setFlash(HttpSession session, String type, String message) {
        if (session == null) return;
        session.setAttribute("flashType", type);
        session.setAttribute("flashMessage", message);
    }

    private static String getSessionRole(HttpSession session) {
        if (session == null) return null;
        Object value = session.getAttribute("role");
        return (value instanceof String) ? (String) value : null;
    }

    private static Integer getSessionUserId(HttpSession session) {
        if (session == null) return null;
        Object value = session.getAttribute("userId");
        return (value instanceof Integer) ? (Integer) value : null;
    }

    private static Integer parseInt(String s) {
        try {
            return (s == null || s.trim().isEmpty()) ? null : Integer.valueOf(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String trim(String s) {
        return s == null ? null : s.trim();
    }
}

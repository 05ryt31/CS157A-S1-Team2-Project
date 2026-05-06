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

@WebServlet(urlPatterns = {
    "/recipient/pickup",
    "/donor/pickup"
})
public class PickupServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        if (!CsrfUtil.isValid(req)) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid CSRF token");
            return;
        }

        HttpSession session = req.getSession(false);
        Integer userId = getSessionUserId(session);
        Integer orgId = getSessionOrgId(session);
        Integer claimId = parseInt(req.getParameter("claimId"));
        String action = trim(req.getParameter("action"));

        if (userId == null || orgId == null) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        if (claimId == null) {
            res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing claim id.");
            return;
        }

        try {
            if ("schedule".equals(action)) {
                LocalDateTime scheduledTime = parseDateTime(req.getParameter("scheduledTime"));
                if (scheduledTime == null) {
                    throw new IllegalStateException("Pickup date and time are required.");
                }

                schedulePickup(userId, orgId, claimId, scheduledTime);
                setFlash(session, "success", "Pickup scheduled successfully.");
                res.sendRedirect(req.getContextPath() + "/recipient/dashboard");
            } else if ("pickedUp".equals(action)) {
                markPickup(userId, orgId, claimId, "PICKED_UP");
                setFlash(session, "success", "Pickup marked as picked up. Listing completed.");
                res.sendRedirect(req.getContextPath() + "/donor/dashboard");
            } else if ("noShow".equals(action)) {
                markPickup(userId, orgId, claimId, "NO_SHOW");
                setFlash(session, "success", "Pickup marked as no-show.");
                res.sendRedirect(req.getContextPath() + "/donor/dashboard");
            } else {
                res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknown pickup action.");
            }
        } catch (IllegalStateException e) {
            setFlash(session, "error", e.getMessage());

            if (req.getServletPath().startsWith("/donor")) {
                res.sendRedirect(req.getContextPath() + "/donor/dashboard");
            } else {
                res.sendRedirect(req.getContextPath() + "/recipient/dashboard");
            }
        } catch (SQLException e) {
            getServletContext().log("Pickup action failed", e);
            setFlash(session, "error", "Could not complete pickup action.");

            if (req.getServletPath().startsWith("/donor")) {
                res.sendRedirect(req.getContextPath() + "/donor/dashboard");
            } else {
                res.sendRedirect(req.getContextPath() + "/recipient/dashboard");
            }
        }
    }

    private static void schedulePickup(int userId, int recipientOrgId, int claimId,
                                       LocalDateTime scheduledTime) throws SQLException {
        try (Connection conn = DBConnection.get()) {
            conn.setAutoCommit(false);

            try {
                String claimStatus;

                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT status FROM claims WHERE claim_id = ? AND org_id = ? FOR UPDATE")) {
                    ps.setInt(1, claimId);
                    ps.setInt(2, recipientOrgId);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new IllegalStateException("Approved claim not found.");
                        }

                        claimStatus = rs.getString("status");
                    }
                }

                if (!"APPROVED".equals(claimStatus)) {
                    throw new IllegalStateException("Only approved claims can be scheduled for pickup.");
                }

                Integer existingPickupId = null;

                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT pickup_id, pickup_status FROM pickups WHERE claim_id = ? FOR UPDATE")) {
                    ps.setInt(1, claimId);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            existingPickupId = rs.getInt("pickup_id");
                            String status = rs.getString("pickup_status");

                            if ("PICKED_UP".equals(status) || "NO_SHOW".equals(status) || "COMPLETED".equals(status)) {
                                throw new IllegalStateException("This pickup can no longer be rescheduled.");
                            }
                        }
                    }
                }

                int pickupId;

                if (existingPickupId == null) {
                    pickupId = nextId(conn, "pickups", "pickup_id");

                    try (PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO pickups (pickup_id, claim_id, scheduled_time, pickup_status, completed_time) "
                          + "VALUES (?, ?, ?, 'SCHEDULED', NULL)")) {
                        ps.setInt(1, pickupId);
                        ps.setInt(2, claimId);
                        ps.setTimestamp(3, Timestamp.valueOf(scheduledTime));
                        ps.executeUpdate();
                    }
                } else {
                    pickupId = existingPickupId;

                    try (PreparedStatement ps = conn.prepareStatement(
                            "UPDATE pickups SET scheduled_time = ?, pickup_status = 'SCHEDULED', completed_time = NULL "
                          + "WHERE pickup_id = ?")) {
                        ps.setTimestamp(1, Timestamp.valueOf(scheduledTime));
                        ps.setInt(2, pickupId);
                        ps.executeUpdate();
                    }
                }

                writeAuditLog(conn, userId, "SCHEDULE", "PICKUP", pickupId);
                conn.commit();
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    private static void markPickup(int userId, int donorOrgId, int claimId, String newStatus)
            throws SQLException {
        try (Connection conn = DBConnection.get()) {
            conn.setAutoCommit(false);

            try {
                int pickupId;
                int listingId;
                String currentPickupStatus;

                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT p.pickup_id, p.pickup_status, l.listing_id "
                      + "FROM pickups p "
                      + "JOIN claims c ON p.claim_id = c.claim_id "
                      + "JOIN listings l ON c.listing_id = l.listing_id "
                      + "WHERE c.claim_id = ? AND l.org_id = ? FOR UPDATE")) {
                    ps.setInt(1, claimId);
                    ps.setInt(2, donorOrgId);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new IllegalStateException("Scheduled pickup not found.");
                        }

                        pickupId = rs.getInt("pickup_id");
                        currentPickupStatus = rs.getString("pickup_status");
                        listingId = rs.getInt("listing_id");
                    }
                }

                if (!"SCHEDULED".equals(currentPickupStatus)) {
                    throw new IllegalStateException("Only scheduled pickups can be updated.");
                }

                Timestamp completedTime = "PICKED_UP".equals(newStatus)
                        ? Timestamp.valueOf(LocalDateTime.now())
                        : null;

                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE pickups SET pickup_status = ?, completed_time = ? WHERE pickup_id = ?")) {
                    ps.setString(1, newStatus);
                    ps.setTimestamp(2, completedTime);
                    ps.setInt(3, pickupId);
                    ps.executeUpdate();
                }

                if ("PICKED_UP".equals(newStatus)) {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "UPDATE listings SET status = 'COMPLETED' WHERE listing_id = ?")) {
                        ps.setInt(1, listingId);
                        ps.executeUpdate();
                    }
                }

                writeAuditLog(conn, userId, newStatus, "PICKUP", pickupId);
                conn.commit();
            } catch (SQLException | RuntimeException e) {
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

    private static Integer getSessionUserId(HttpSession session) {
        if (session == null) return null;
        Object value = session.getAttribute("userId");
        return (value instanceof Integer) ? (Integer) value : null;
    }

    private static Integer getSessionOrgId(HttpSession session) {
        if (session == null) return null;
        Object value = session.getAttribute("orgId");
        return (value instanceof Integer) ? (Integer) value : null;
    }

    private static Integer parseInt(String s) {
        try {
            return (s == null || s.trim().isEmpty()) ? null : Integer.valueOf(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static LocalDateTime parseDateTime(String s) {
        try {
            return (s == null || s.trim().isEmpty()) ? null : LocalDateTime.parse(s.trim());
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static String trim(String s) {
        return s == null ? null : s.trim();
    }
}
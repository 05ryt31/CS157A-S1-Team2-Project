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
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet("/donor/listings")
public class DonorListingsServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        Integer orgId = getSessionOrgId(req);
        if (orgId == null) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN, "You are not authorized to view this page.");
            return;
        }

        try {
            req.setAttribute("listings", loadListings(orgId));
            req.setAttribute("csrfToken", CsrfUtil.getOrCreate(req));
            req.getRequestDispatcher("/WEB-INF/views/donor-listings.jsp").forward(req, res);
        } catch (SQLException e) {
            getServletContext().log("Failed to load donor listings", e);
            res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Could not load your listings.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        if (!CsrfUtil.isValid(req)) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid CSRF token");
            return;
        }

        String action = req.getParameter("action");
        if (!"cancel".equalsIgnoreCase(action)) {
            res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknown action.");
            return;
        }

        Integer orgId = getSessionOrgId(req);
        Integer userId = getSessionUserId(req);
        if (orgId == null || userId == null) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN, "You are not authorized to perform this action.");
            return;
        }

        String listingIdParam = req.getParameter("listingId");
        if (listingIdParam == null) {
            res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing listing id.");
            return;
        }

        int listingId;
        try {
            listingId = Integer.parseInt(listingIdParam);
        } catch (NumberFormatException e) {
            res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid listing id.");
            return;
        }

        try {
            boolean cancelled = cancelListing(listingId, orgId);
            if (!cancelled) {
                res.sendError(HttpServletResponse.SC_FORBIDDEN, "Cannot cancel this listing.");
                return;
            }
            writeAuditLog(userId, "CANCEL", "LISTING", listingId);
            res.sendRedirect(req.getContextPath() + "/donor/listings");
        } catch (SQLException e) {
            getServletContext().log("Failed to cancel listing", e);
            res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Could not cancel the listing.");
        }
    }

    private static List<ListingRow> loadListings(int orgId) throws SQLException {
        String sql = "SELECT l.listing_id, l.title, l.status, l.created_at, "
                   + "loc.address, loc.city, loc.zip, "
                   + "COALESCE(SUM(li.quantity), 0) AS total_quantity, "
                   + "MIN(li.expiry_date) AS earliest_expiry "
                   + "FROM listings l "
                   + "JOIN location loc ON l.location_id = loc.location_id "
                   + "LEFT JOIN listingitems li ON l.listing_id = li.listing_id "
                   + "WHERE l.org_id = ? "
                   + "GROUP BY l.listing_id, l.title, l.status, l.created_at, loc.address, loc.city, loc.zip "
                   + "ORDER BY l.created_at DESC";

        try (Connection conn = DBConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orgId);
            try (ResultSet rs = ps.executeQuery()) {
                List<ListingRow> rows = new ArrayList<>();
                while (rs.next()) {
                    ListingRow row = new ListingRow();
                    row.listingId = rs.getInt("listing_id");
                    row.title = rs.getString("title");
                    row.status = rs.getString("status");
                    Timestamp createdAt = rs.getTimestamp("created_at");
                    row.createdAt = createdAt == null ? "" : createdAt.toString();
                    row.address = rs.getString("address");
                    row.city = rs.getString("city");
                    row.zip = rs.getString("zip");
                    row.totalQuantity = rs.getInt("total_quantity");
                    row.earliestExpiry = rs.getDate("earliest_expiry") == null ? "N/A" : rs.getDate("earliest_expiry").toString();
                    rows.add(row);
                }
                return rows;
            }
        }
    }

    private static boolean cancelListing(int listingId, int orgId) throws SQLException {
        String sql = "UPDATE listings SET status = 'CANCELLED' "
                   + "WHERE listing_id = ? AND org_id = ? AND status = 'OPEN'";
        try (Connection conn = DBConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, listingId);
            ps.setInt(2, orgId);
            return ps.executeUpdate() == 1;
        }
    }

    private static void writeAuditLog(int userId, String actionType, String entityType, int entityId)
            throws SQLException {
        try (Connection conn = DBConnection.get()) {
            conn.setAutoCommit(false);
            try {
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
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
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

    private static Integer getSessionOrgId(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) return null;
        return (Integer) session.getAttribute("orgId");
    }

    private static Integer getSessionUserId(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) return null;
        return (Integer) session.getAttribute("userId");
    }

    private static final class ListingRow {
        int listingId;
        String title;
        String status;
        String createdAt;
        String address;
        String city;
        String zip;
        int totalQuantity;
        String earliestExpiry;
    }
}

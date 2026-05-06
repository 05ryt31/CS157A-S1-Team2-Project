package com.flowchain.servlet;

import com.flowchain.db.DBConnection;
import com.flowchain.util.CsrfUtil;

import java.io.IOException;
import java.sql.Connection;
import java.sql.Date;
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

@WebServlet("/donor/listings/detail")
public class DonorListingDetailServlet extends HttpServlet {

    public static class ListingDetail {
        private int listingId;
        private String title;
        private String description;
        private String status;
        private Timestamp createdAt;
        private String address;
        private String city;
        private String zip;

        public int getListingId() { return listingId; }
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public String getStatus() { return status; }
        public Timestamp getCreatedAt() { return createdAt; }
        public String getAddress() { return address; }
        public String getCity() { return city; }
        public String getZip() { return zip; }
    }

    public static class ListingItemRow {
        private String categoryName;
        private int quantity;
        private String unit;
        private Date expiryDate;

        public String getCategoryName() { return categoryName; }
        public int getQuantity() { return quantity; }
        public String getUnit() { return unit; }
        public Date getExpiryDate() { return expiryDate; }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        Integer orgId = getSessionOrgId(req.getSession(false));
        Integer listingId = parseInt(req.getParameter("id"));

        if (orgId == null || listingId == null) {
            res.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        try {
            ListingDetail listing = loadListing(orgId, listingId);
            if (listing == null) {
                res.sendError(HttpServletResponse.SC_NOT_FOUND, "Listing not found.");
                return;
            }

            req.setAttribute("csrfToken", CsrfUtil.getOrCreate(req));
            req.setAttribute("listing", listing);
            req.setAttribute("items", loadItems(listingId));
            req.getRequestDispatcher("/WEB-INF/views/donor-listing-detail.jsp").forward(req, res);
        } catch (SQLException e) {
            getServletContext().log("Failed to load donor listing detail", e);
            res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        if (!CsrfUtil.isValid(req)) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid CSRF token");
            return;
        }

        HttpSession session = req.getSession(false);
        Integer orgId = getSessionOrgId(session);
        Integer userId = getSessionUserId(session);
        Integer listingId = parseInt(req.getParameter("listingId"));
        String action = trim(req.getParameter("action"));

        if (orgId == null || userId == null || listingId == null) {
            res.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        try {
            if ("cancel".equals(action)) {
                cancelListing(userId, orgId, listingId);
            }

            res.sendRedirect(req.getContextPath() + "/donor/listings/detail?id=" + listingId);
        } catch (SQLException e) {
            getServletContext().log("Failed to update donor listing", e);
            res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private static ListingDetail loadListing(int orgId, int listingId) throws SQLException {
        String sql = "SELECT l.listing_id, l.title, l.description, l.status, l.created_at, "
                   + "loc.address, loc.city, loc.zip "
                   + "FROM listings l "
                   + "JOIN location loc ON l.location_id = loc.location_id "
                   + "WHERE l.org_id = ? AND l.listing_id = ?";

        try (Connection conn = DBConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, orgId);
            ps.setInt(2, listingId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;

                ListingDetail row = new ListingDetail();
                row.listingId = rs.getInt("listing_id");
                row.title = rs.getString("title");
                row.description = rs.getString("description");
                row.status = rs.getString("status");
                row.createdAt = rs.getTimestamp("created_at");
                row.address = rs.getString("address");
                row.city = rs.getString("city");
                row.zip = rs.getString("zip");
                return row;
            }
        }
    }

    private static List<ListingItemRow> loadItems(int listingId) throws SQLException {
        String sql = "SELECT fc.category_name, li.quantity, li.unit, li.expiry_date "
                   + "FROM listingitems li "
                   + "JOIN foodcategories fc ON li.category_id = fc.category_id "
                   + "WHERE li.listing_id = ? "
                   + "ORDER BY li.expiry_date ASC, fc.category_name ASC";

        List<ListingItemRow> rows = new ArrayList<>();

        try (Connection conn = DBConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, listingId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ListingItemRow row = new ListingItemRow();
                    row.categoryName = rs.getString("category_name");
                    row.quantity = rs.getInt("quantity");
                    row.unit = rs.getString("unit");
                    row.expiryDate = rs.getDate("expiry_date");
                    rows.add(row);
                }
            }
        }

        return rows;
    }

    private static void cancelListing(int userId, int orgId, int listingId) throws SQLException {
        try (Connection conn = DBConnection.get()) {
            conn.setAutoCommit(false);

            try {
                String status = null;

                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT status FROM listings WHERE listing_id = ? AND org_id = ? FOR UPDATE")) {
                    ps.setInt(1, listingId);
                    ps.setInt(2, orgId);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException("Listing not found.");
                        }
                        status = rs.getString("status");
                    }
                }

                // donor can only cancel listings that are still open
                if (!"OPEN".equals(status)) {
                    conn.rollback();
                    return;
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE listings SET status = 'CANCELLED' WHERE listing_id = ? AND org_id = ?")) {
                    ps.setInt(1, listingId);
                    ps.setInt(2, orgId);
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE claims SET status = 'REJECTED' "
                      + "WHERE listing_id = ? AND status IN ('REQUESTED', 'PENDING')")) {
                    ps.setInt(1, listingId);
                    ps.executeUpdate();
                }

                writeAuditLog(conn, userId, "CANCEL", "LISTING", listingId);

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

    private static void writeAuditLog(Connection conn, int userId, String actionType, String entityType, int entityId)
            throws SQLException {
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

    private static Integer getSessionOrgId(HttpSession session) {
        if (session == null) return null;
        Object value = session.getAttribute("orgId");
        return (value instanceof Integer) ? (Integer) value : null;
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
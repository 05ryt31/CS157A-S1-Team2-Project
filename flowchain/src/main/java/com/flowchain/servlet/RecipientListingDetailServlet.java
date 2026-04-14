package com.flowchain.servlet;
import com.flowchain.db.DBConnection;

import java.io.IOException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet("/recipient/listing")
public class RecipientListingDetailServlet extends HttpServlet {

    private static final class ListingDetail {
        int listingId;
        String title;
        String description;
        String status;
        Timestamp createdAt;
        String orgName;
        String phone;
        String address;
        String city;
        String zip;
    }

    private static final class ListingItemRow {
        int listingItemId;
        String categoryName;
        int quantity;
        String unit;
        Date expiryDate;
    }

    private static final class ClaimStatusRow {
        Integer claimId;
        String claimStatus;
        Timestamp claimedAt;
        String pickupStatus;
        Timestamp scheduledTime;
        Timestamp completedTime;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        String idParam = req.getParameter("id");
        if (idParam == null || idParam.trim().isEmpty()) {
            res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing listing id.");
            return;
        }

        int listingId;
        try {
            listingId = Integer.parseInt(idParam);
        } catch (NumberFormatException e) {
            res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid listing id.");
            return;
        }

        Integer orgId = getSessionOrgId(req.getSession(false));

        try {
            ListingDetail detail = loadListingDetail(listingId);
            if (detail == null) {
                res.sendError(HttpServletResponse.SC_NOT_FOUND, "Listing not found.");
                return;
            }

            req.setAttribute("listing", detail);
            req.setAttribute("items", loadListingItems(listingId));

            if (orgId != null) {
                req.setAttribute("myClaim", loadRecipientClaimStatus(listingId, orgId));
            }

            req.getRequestDispatcher("/WEB-INF/views/recipient-listing-detail.jsp").forward(req, res);
        } catch (SQLException e) {
            getServletContext().log("Failed to load listing detail", e);
            res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Could not load listing detail.");
        }
    }

    private static ListingDetail loadListingDetail(int listingId) throws SQLException {
        String sql = "SELECT l.listing_id, l.title, l.description, l.status, l.created_at, " +
                     "o.org_name, o.phone, loc.address, loc.city, loc.zip " +
                     "FROM listings l " +
                     "JOIN organizations o ON l.org_id = o.org_id " +
                     "JOIN location loc ON l.location_id = loc.location_id " +
                     "WHERE l.listing_id = ?";

        try (Connection conn = DBConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, listingId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                ListingDetail detail = new ListingDetail();
                detail.listingId = rs.getInt("listing_id");
                detail.title = rs.getString("title");
                detail.description = rs.getString("description");
                detail.status = rs.getString("status");
                detail.createdAt = rs.getTimestamp("created_at");
                detail.orgName = rs.getString("org_name");
                detail.phone = rs.getString("phone");
                detail.address = rs.getString("address");
                detail.city = rs.getString("city");
                detail.zip = rs.getString("zip");
                return detail;
            }
        }
    }

    private static List<ListingItemRow> loadListingItems(int listingId) throws SQLException {
        String sql = "SELECT li.listing_item_id, fc.category_name, li.quantity, li.unit, li.expiry_date " +
                     "FROM listingitems li " +
                     "JOIN foodcategories fc ON li.category_id = fc.category_id " +
                     "WHERE li.listing_id = ? " +
                     "ORDER BY li.expiry_date ASC, fc.category_name ASC";

        List<ListingItemRow> rows = new ArrayList<>();

        try (Connection conn = DBConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, listingId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ListingItemRow row = new ListingItemRow();
                    row.listingItemId = rs.getInt("listing_item_id");
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

    private static ClaimStatusRow loadRecipientClaimStatus(int listingId, int orgId) throws SQLException {
        String sql = "SELECT c.claim_id, c.status AS claim_status, c.claimed_at, " +
                     "p.pickup_status, p.scheduled_time, p.completed_time " +
                     "FROM claims c " +
                     "LEFT JOIN pickups p ON c.claim_id = p.claim_id " +
                     "WHERE c.listing_id = ? AND c.org_id = ? " +
                     "ORDER BY c.claimed_at DESC " +
                     "LIMIT 1";

        try (Connection conn = DBConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, listingId);
            ps.setInt(2, orgId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                ClaimStatusRow row = new ClaimStatusRow();
                row.claimId = rs.getInt("claim_id");
                row.claimStatus = rs.getString("claim_status");
                row.claimedAt = rs.getTimestamp("claimed_at");
                row.pickupStatus = rs.getString("pickup_status");
                row.scheduledTime = rs.getTimestamp("scheduled_time");
                row.completedTime = rs.getTimestamp("completed_time");
                return row;
            }
        }
    }

    private static Integer getSessionOrgId(HttpSession session) {
        if (session == null) return null;
        Object value = session.getAttribute("orgId");
        return (value instanceof Integer) ? (Integer) value : null;
    }
}

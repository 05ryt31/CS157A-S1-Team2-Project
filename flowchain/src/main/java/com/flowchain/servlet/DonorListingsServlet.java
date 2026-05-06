package com.flowchain.servlet;

import com.flowchain.db.DBConnection;

import java.io.IOException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

// shows all listings created by the donor

@WebServlet("/donor/listings")
public class DonorListingsServlet extends HttpServlet {

    public static class ListingRow {
        private int listingId;
        private String title;
        private String status;
        private Date earliestExpiry;
        private int totalQuantity;

        public int getListingId() { return listingId; }
        public String getTitle() { return title; }
        public String getStatus() { return status; }
        public Date getEarliestExpiry() { return earliestExpiry; }
        public int getTotalQuantity() { return totalQuantity; }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        // this makes sure the donor is logged in
        Integer orgId = getSessionOrgId(req.getSession(false));
        if (orgId == null) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        try {
            // load listings and send to JSP
            req.setAttribute("listings", loadListings(orgId));
            req.getRequestDispatcher("/WEB-INF/views/donor-listings.jsp").forward(req, res);
        } catch (SQLException e) {
            getServletContext().log("Failed to load donor listings", e);
            res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    // basically loads all listings created by this donor org
    private static List<ListingRow> loadListings(int orgId) throws SQLException {
        String sql =
            "SELECT l.listing_id, l.title, l.status, " +
            "MIN(li.expiry_date) AS earliest_expiry, " +
            "COALESCE(SUM(li.quantity),0) AS total_quantity " +
            "FROM listings l " +
            "LEFT JOIN listingitems li ON l.listing_id = li.listing_id " +
            "WHERE l.org_id = ? " +
            "GROUP BY l.listing_id, l.title, l.status, l.created_at " +
            "ORDER BY l.created_at DESC";

        List<ListingRow> rows = new ArrayList<>();

        try (Connection conn = DBConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, orgId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ListingRow row = new ListingRow();
                    row.listingId = rs.getInt("listing_id");
                    row.title = rs.getString("title");
                    row.status = rs.getString("status");
                    row.earliestExpiry = rs.getDate("earliest_expiry");
                    row.totalQuantity = rs.getInt("total_quantity");
                    rows.add(row);
                }
            }
        }

        return rows;
    }

    // this gets the org ID from the session
    private static Integer getSessionOrgId(HttpSession session) {
        if (session == null) return null;
        Object value = session.getAttribute("orgId");
        return (value instanceof Integer) ? (Integer) value : null;
    }
}
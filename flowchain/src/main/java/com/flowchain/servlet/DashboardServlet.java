package com.flowchain.servlet;

import com.flowchain.db.DBConnection;
import com.flowchain.util.CsrfUtil;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet(urlPatterns = {
    "/donor/dashboard",
    "/recipient/dashboard",
    "/admin/dashboard"
})
public class DashboardServlet extends HttpServlet {

    public static class DonorClaimRow {
        private int claimId;
        private int listingId;
        private String listingTitle;
        private String recipientOrgName;
        private Timestamp claimedAt;
        private String claimStatus;
        private Integer pickupId;
        private String pickupStatus;
        private Timestamp scheduledTime;
        private Timestamp completedTime;

        public int getClaimId() {
            return claimId;
        }

        public int getListingId() {
            return listingId;
        }

        public String getListingTitle() {
            return listingTitle;
        }

        public String getRecipientOrgName() {
            return recipientOrgName;
        }

        public Timestamp getClaimedAt() {
            return claimedAt;
        }

        public String getClaimStatus() {
            return claimStatus;
        }

        public Integer getPickupId() {
            return pickupId;
        }

        public String getPickupStatus() {
            return pickupStatus;
        }

        public Timestamp getScheduledTime() {
            return scheduledTime;
        }

        public Timestamp getCompletedTime() {
            return completedTime;
        }
    }

    public static class RecipientClaimRow {
        private int claimId;
        private int listingId;
        private String listingTitle;
        private String donorOrgName;
        private Timestamp claimedAt;
        private String claimStatus;
        private Integer pickupId;
        private String pickupStatus;
        private Timestamp scheduledTime;
        private Timestamp completedTime;

        public int getClaimId() {
            return claimId;
        }

        public int getListingId() {
            return listingId;
        }

        public String getListingTitle() {
            return listingTitle;
        }

        public String getDonorOrgName() {
            return donorOrgName;
        }

        public Timestamp getClaimedAt() {
            return claimedAt;
        }

        public String getClaimStatus() {
            return claimStatus;
        }

        public Integer getPickupId() {
            return pickupId;
        }

        public String getPickupStatus() {
            return pickupStatus;
        }

        public Timestamp getScheduledTime() {
            return scheduledTime;
        }

        public Timestamp getCompletedTime() {
            return completedTime;
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        String path = req.getServletPath();

        req.setAttribute("csrfToken", CsrfUtil.getOrCreate(req));
        moveFlashToRequest(req);

        try {
            if (path.startsWith("/donor")) {
                Integer orgId = getSessionOrgId(req.getSession(false));
                req.setAttribute("claims", orgId == null ? Collections.emptyList() : loadDonorClaims(orgId));
                req.getRequestDispatcher("/WEB-INF/views/donor-dashboard.jsp").forward(req, res);
            } else if (path.startsWith("/recipient")) {
                Integer orgId = getSessionOrgId(req.getSession(false));
                req.setAttribute("myClaims", orgId == null ? Collections.emptyList() : loadRecipientClaims(orgId));
                req.getRequestDispatcher("/WEB-INF/views/recipient-dashboard.jsp").forward(req, res);
            } else {
                req.getRequestDispatcher("/WEB-INF/views/admin-dashboard.jsp").forward(req, res);
            }
        } catch (SQLException e) {
            getServletContext().log("Failed to load dashboard", e);
            res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Could not load dashboard.");
        }
    }

    private static List<DonorClaimRow> loadDonorClaims(int donorOrgId) throws SQLException {
        String sql = "SELECT c.claim_id, c.listing_id, l.title AS listing_title, "
                   + "o.org_name AS recipient_org_name, c.claimed_at, c.status AS claim_status, "
                   + "p.pickup_id, p.pickup_status, p.scheduled_time, p.completed_time "
                   + "FROM claims c "
                   + "JOIN listings l ON c.listing_id = l.listing_id "
                   + "JOIN organizations o ON c.org_id = o.org_id "
                   + "LEFT JOIN pickups p ON c.claim_id = p.claim_id "
                   + "WHERE l.org_id = ? "
                   + "ORDER BY c.claimed_at DESC";

        List<DonorClaimRow> rows = new ArrayList<>();

        try (Connection conn = DBConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, donorOrgId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DonorClaimRow row = new DonorClaimRow();
                    row.claimId = rs.getInt("claim_id");
                    row.listingId = rs.getInt("listing_id");
                    row.listingTitle = rs.getString("listing_title");
                    row.recipientOrgName = rs.getString("recipient_org_name");
                    row.claimedAt = rs.getTimestamp("claimed_at");
                    row.claimStatus = rs.getString("claim_status");

                    int pickupId = rs.getInt("pickup_id");
                    row.pickupId = rs.wasNull() ? null : pickupId;

                    row.pickupStatus = rs.getString("pickup_status");
                    row.scheduledTime = rs.getTimestamp("scheduled_time");
                    row.completedTime = rs.getTimestamp("completed_time");
                    rows.add(row);
                }
            }
        }

        return rows;
    }

    private static List<RecipientClaimRow> loadRecipientClaims(int recipientOrgId) throws SQLException {
        String sql = "SELECT c.claim_id, c.listing_id, l.title AS listing_title, "
                   + "o.org_name AS donor_org_name, c.claimed_at, c.status AS claim_status, "
                   + "p.pickup_id, p.pickup_status, p.scheduled_time, p.completed_time "
                   + "FROM claims c "
                   + "JOIN listings l ON c.listing_id = l.listing_id "
                   + "JOIN organizations o ON l.org_id = o.org_id "
                   + "LEFT JOIN pickups p ON c.claim_id = p.claim_id "
                   + "WHERE c.org_id = ? "
                   + "ORDER BY c.claimed_at DESC";

        List<RecipientClaimRow> rows = new ArrayList<>();

        try (Connection conn = DBConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, recipientOrgId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    RecipientClaimRow row = new RecipientClaimRow();
                    row.claimId = rs.getInt("claim_id");
                    row.listingId = rs.getInt("listing_id");
                    row.listingTitle = rs.getString("listing_title");
                    row.donorOrgName = rs.getString("donor_org_name");
                    row.claimedAt = rs.getTimestamp("claimed_at");
                    row.claimStatus = rs.getString("claim_status");

                    int pickupId = rs.getInt("pickup_id");
                    row.pickupId = rs.wasNull() ? null : pickupId;

                    row.pickupStatus = rs.getString("pickup_status");
                    row.scheduledTime = rs.getTimestamp("scheduled_time");
                    row.completedTime = rs.getTimestamp("completed_time");
                    rows.add(row);
                }
            }
        }

        return rows;
    }

    private static void moveFlashToRequest(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) return;

        Object type = session.getAttribute("flashType");
        Object message = session.getAttribute("flashMessage");

        if (type != null && message != null) {
            req.setAttribute(String.valueOf(type), message);
        }

        session.removeAttribute("flashType");
        session.removeAttribute("flashMessage");
    }

    private static Integer getSessionOrgId(HttpSession session) {
        if (session == null) return null;
        Object value = session.getAttribute("orgId");
        return (value instanceof Integer) ? (Integer) value : null;
    }
}
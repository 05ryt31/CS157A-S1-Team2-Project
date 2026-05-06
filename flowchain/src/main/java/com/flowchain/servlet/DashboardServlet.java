package com.flowchain.servlet;

import com.flowchain.db.DBConnection;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

@WebServlet(urlPatterns = {
    "/donor/dashboard",
    "/recipient/dashboard",
    "/admin/dashboard"
})
public class DashboardServlet extends HttpServlet {

    public static class PendingClaimRow {
        private int claimId;
        private int listingId;
        private String title;
        private String recipientOrgName;
        private Timestamp claimedAt;

        public int getClaimId() { return claimId; }
        public int getListingId() { return listingId; }
        public String getTitle() { return title; }
        public String getRecipientOrgName() { return recipientOrgName; }
        public Timestamp getClaimedAt() { return claimedAt; }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        String path = req.getServletPath();
        String view;

        if (path.startsWith("/donor")) {
            Integer orgId = getSessionOrgId(req.getSession(false));

            if (orgId != null) {
                try {
                    req.setAttribute("pendingClaims", loadPendingClaims(orgId));
                } catch (SQLException e) {
                    throw new ServletException("Could not load pending claims.", e);
                }
            }

            view = "/WEB-INF/views/donor-dashboard.jsp";

        } else if (path.startsWith("/recipient")) {
            view = "/WEB-INF/views/recipient-dashboard.jsp";
        } else {
            view = "/WEB-INF/views/admin-dashboard.jsp";
        }

        req.getRequestDispatcher(view).forward(req, res);
    }

    private static List<PendingClaimRow> loadPendingClaims(int donorOrgId) throws SQLException {
        String sql = "SELECT c.claim_id, c.listing_id, c.claimed_at, l.title, o.org_name AS recipient_org_name " +
                     "FROM claims c " +
                     "JOIN listings l ON c.listing_id = l.listing_id " +
                     "JOIN organizations o ON c.org_id = o.org_id " +
                     "WHERE l.org_id = ? AND c.status = 'PENDING' " +
                     "ORDER BY c.claimed_at DESC";

        List<PendingClaimRow> rows = new ArrayList<>();

        try (Connection conn = DBConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, donorOrgId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    PendingClaimRow row = new PendingClaimRow();
                    row.claimId = rs.getInt("claim_id");
                    row.listingId = rs.getInt("listing_id");
                    row.claimedAt = rs.getTimestamp("claimed_at");
                    row.title = rs.getString("title");
                    row.recipientOrgName = rs.getString("recipient_org_name");
                    rows.add(row);
                }
            }
        }

        return rows;
    }

    private static Integer getSessionOrgId(HttpSession session) {
        if (session == null) return null;
        Object value = session.getAttribute("orgId");
        return (value instanceof Integer) ? (Integer) value : null;
    }
}
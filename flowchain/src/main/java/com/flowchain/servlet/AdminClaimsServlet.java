package com.flowchain.servlet;

import com.flowchain.db.DBConnection;
import com.flowchain.util.CsrfUtil;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

@WebServlet("/admin/claims")
public class AdminClaimsServlet extends HttpServlet {

    public static class ClaimRow {

        private int claimId;
        private String listingTitle;
        private String donorOrg;
        private String recipientOrg;
        private String status;

        public int getClaimId() {
            return claimId;
        }

        public String getListingTitle() {
            return listingTitle;
        }

        public String getDonorOrg() {
            return donorOrg;
        }

        public String getRecipientOrg() {
            return recipientOrg;
        }

        public String getStatus() {
            return status;
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        try {

            req.setAttribute("csrfToken", CsrfUtil.getOrCreate(req));
            moveFlashToRequest(req);
            req.setAttribute("claims", loadClaims());

            req.getRequestDispatcher("/WEB-INF/views/admin-claims.jsp")
                    .forward(req, res);

        } catch (SQLException e) {

            getServletContext().log("Failed to load claims", e);

            res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private static List<ClaimRow> loadClaims() throws SQLException {

        String sql =
                "SELECT c.claim_id, l.title, " +
                "do.org_name AS donor_org, " +
                "ro.org_name AS recipient_org, " +
                "c.status " +
                "FROM claims c " +
                "JOIN listings l ON c.listing_id = l.listing_id " +
                "JOIN organizations do ON l.org_id = do.org_id " +
                "JOIN organizations ro ON c.org_id = ro.org_id " +
                "ORDER BY c.claimed_at DESC";

        List<ClaimRow> rows = new ArrayList<>();

        try (Connection conn = DBConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {

                ClaimRow row = new ClaimRow();

                row.claimId = rs.getInt("claim_id");
                row.listingTitle = rs.getString("title");
                row.donorOrg = rs.getString("donor_org");
                row.recipientOrg = rs.getString("recipient_org");
                row.status = rs.getString("status");

                rows.add(row);
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
}

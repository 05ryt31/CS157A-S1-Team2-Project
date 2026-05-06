package com.flowchain.servlet;

import com.flowchain.db.DBConnection;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.*;

@WebServlet("/recipient/claim/cancel")
public class CancelClaimServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        HttpSession session = req.getSession(false);
        Integer orgId = session == null ? null : (Integer) session.getAttribute("orgId");

        if (orgId == null) {
            res.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        int claimId = Integer.parseInt(req.getParameter("claimId"));

        try (Connection conn = DBConnection.get()) {
            conn.setAutoCommit(false);

            ClaimData claim = getClaim(conn, claimId, orgId);

            if (claim != null && ("PENDING".equals(claim.status) || "APPROVED".equals(claim.status))) {
                updateClaimStatus(conn, claimId, "CANCELLED");

                if ("APPROVED".equals(claim.status)) {
                    reopenListing(conn, claim.listingId);
                }
            }

            conn.commit();

            int redirectListingId = claim == null ? 0 : claim.listingId;
            res.sendRedirect(req.getContextPath() + "/recipient/listing?id=" + redirectListingId);

        } catch (SQLException e) {
            throw new ServletException("Could not cancel claim.", e);
        }
    }

    private static ClaimData getClaim(Connection conn, int claimId, int orgId) throws SQLException {
        String sql = "SELECT claim_id, listing_id, status FROM claims WHERE claim_id = ? AND org_id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, claimId);
            ps.setInt(2, orgId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                ClaimData data = new ClaimData();
                data.listingId = rs.getInt("listing_id");
                data.status = rs.getString("status");
                return data;
            }
        }
    }

    private static void updateClaimStatus(Connection conn, int claimId, String status) throws SQLException {
        String sql = "UPDATE claims SET status = ? WHERE claim_id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, claimId);
            ps.executeUpdate();
        }
    }

    private static void reopenListing(Connection conn, int listingId) throws SQLException {
        String sql = "UPDATE listings SET status = 'OPEN' WHERE listing_id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, listingId);
            ps.executeUpdate();
        }
    }

    private static class ClaimData {
        int listingId;
        String status;
    }
}
package com.flowchain.servlet;

import com.flowchain.db.DBConnection;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.*;

@WebServlet("/donor/claim/update")
public class UpdateClaimStatusServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        int claimId = Integer.parseInt(req.getParameter("claimId"));
        String action = req.getParameter("action");

        try (Connection conn = DBConnection.get()) {
            conn.setAutoCommit(false);

            if ("approve".equals(action)) {
                approveClaim(conn, claimId);
            } else if ("reject".equals(action)) {
                updateClaimStatus(conn, claimId, "REJECTED");
            }

            conn.commit();
            res.sendRedirect(req.getContextPath() + "/donor/dashboard");

        } catch (SQLException e) {
            throw new ServletException("Could not update claim status.", e);
        }
    }

    private static void approveClaim(Connection conn, int claimId) throws SQLException {
        int listingId = getListingId(conn, claimId);

        updateClaimStatus(conn, claimId, "APPROVED");

        String sql = "UPDATE listings SET status = 'CLAIMED' WHERE listing_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, listingId);
            ps.executeUpdate();
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

    private static int getListingId(Connection conn, int claimId) throws SQLException {
        String sql = "SELECT listing_id FROM claims WHERE claim_id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, claimId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("Claim not found.");
                }
                return rs.getInt("listing_id");
            }
        }
    }
}
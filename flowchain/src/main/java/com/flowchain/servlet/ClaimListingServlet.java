package com.flowchain.servlet;

import com.flowchain.db.DBConnection;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.*;

@WebServlet("/recipient/claim")
public class ClaimListingServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        HttpSession session = req.getSession(false);
        Integer orgId = session == null ? null : (Integer) session.getAttribute("orgId");

        if (orgId == null) {
            res.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        int listingId = Integer.parseInt(req.getParameter("listingId"));

        try (Connection conn = DBConnection.get()) {
            conn.setAutoCommit(false);

            if (hasActiveClaim(conn, listingId, orgId)) {
                res.sendRedirect(req.getContextPath() + "/recipient/listing?id=" + listingId);
                return;
            }

            int claimId = nextId(conn, "claims", "claim_id");

            String sql = "INSERT INTO claims (claim_id, listing_id, org_id, claimed_at, status) " +
                         "VALUES (?, ?, ?, NOW(), 'PENDING')";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, claimId);
                ps.setInt(2, listingId);
                ps.setInt(3, orgId);
                ps.executeUpdate();
            }

            conn.commit();
            res.sendRedirect(req.getContextPath() + "/recipient/listing?id=" + listingId);

        } catch (SQLException e) {
            throw new ServletException("Could not submit claim.", e);
        }
    }

    private static boolean hasActiveClaim(Connection conn, int listingId, int orgId) throws SQLException {
        String sql = "SELECT claim_id FROM claims " +
                     "WHERE listing_id = ? AND org_id = ? " +
                     "AND status IN ('PENDING', 'APPROVED')";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, listingId);
            ps.setInt(2, orgId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static int nextId(Connection conn, String table, String column) throws SQLException {
        String sql = "SELECT COALESCE(MAX(" + column + "), 0) + 1 AS next_id FROM " + table;

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            rs.next();
            return rs.getInt("next_id");
        }
    }
}
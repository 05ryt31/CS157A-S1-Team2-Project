package com.flowchain.servlet;

import com.flowchain.db.DBConnection;
import com.flowchain.util.CsrfUtil;

import java.io.IOException;
import java.sql.Connection;
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

@WebServlet("/donor/listings/new")
public class DonorListingNewServlet extends HttpServlet {

    public static class LocationRow {
        private int locationId;
        private String address;
        private String city;
        private String zip;

        public int getLocationId() {
            return locationId;
        }

        public String getAddress() {
            return address;
        }

        public String getCity() {
            return city;
        }

        public String getZip() {
            return zip;
        }
    }

    public static class CategoryRow {
        private int categoryId;
        private String categoryName;

        public int getCategoryId() {
            return categoryId;
        }

        public String getCategoryName() {
            return categoryName;
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        Integer orgId = getSessionOrgId(req.getSession(false));
        if (orgId == null) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        try {
            req.setAttribute("csrfToken", CsrfUtil.getOrCreate(req));
            req.setAttribute("locations", loadLocations(orgId));
            req.setAttribute("categories", loadCategories());
            req.getRequestDispatcher("/WEB-INF/views/donor-listing-new.jsp").forward(req, res);
        } catch (SQLException e) {
            getServletContext().log("Failed to load donor listing form", e);
            res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Could not load listing form.");
        }
    }

    private static List<LocationRow> loadLocations(int orgId) throws SQLException {
        String sql = "SELECT location_id, address, city, zip "
                   + "FROM location "
                   + "WHERE org_id = ? "
                   + "ORDER BY city, address";

        List<LocationRow> rows = new ArrayList<>();

        try (Connection conn = DBConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, orgId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LocationRow row = new LocationRow();
                    row.locationId = rs.getInt("location_id");
                    row.address = rs.getString("address");
                    row.city = rs.getString("city");
                    row.zip = rs.getString("zip");
                    rows.add(row);
                }
            }
        }

        return rows;
    }

    private static List<CategoryRow> loadCategories() throws SQLException {
        String sql = "SELECT category_id, category_name "
                   + "FROM foodcategories "
                   + "ORDER BY category_name";

        List<CategoryRow> rows = new ArrayList<>();

        try (Connection conn = DBConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                CategoryRow row = new CategoryRow();
                row.categoryId = rs.getInt("category_id");
                row.categoryName = rs.getString("category_name");
                rows.add(row);
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
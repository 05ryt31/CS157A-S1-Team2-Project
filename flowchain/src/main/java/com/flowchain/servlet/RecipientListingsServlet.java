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

@WebServlet("/recipient/listings")
public class RecipientListingsServlet extends HttpServlet {

    private static final class CategoryRow {
        int categoryId;
        String categoryName;
    }

    private static final class ListingRow {
        int listingId;
        String title;
        String orgName;
        String city;
        String status;
        int totalQuantity;
        String categories;
        Date earliestExpiry;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        String q = trim(req.getParameter("q"));
        String category = trim(req.getParameter("category"));
        String city = trim(req.getParameter("city"));

        try {
            req.setAttribute("categories", loadCategories());
            req.setAttribute("cities", loadCities());
            req.setAttribute("listings", loadListings(q, category, city));
            req.setAttribute("q", q);
            req.setAttribute("selectedCategory", category);
            req.setAttribute("selectedCity", city);

            req.getRequestDispatcher("/WEB-INF/views/recipient-listings.jsp").forward(req, res);
        } catch (SQLException e) {
            getServletContext().log("Failed to load recipient listings", e);
            res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Could not load listings.");
        }
    }

    private static List<CategoryRow> loadCategories() throws SQLException {
        String sql = "SELECT category_id, category_name " +
                     "FROM foodcategories " +
                     "ORDER BY category_name";

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

    private static List<String> loadCities() throws SQLException {
        String sql = "SELECT DISTINCT city FROM location ORDER BY city";
        List<String> cities = new ArrayList<>();

        try (Connection conn = DBConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                cities.add(rs.getString("city"));
            }
        }

        return cities;
    }

    private static List<ListingRow> loadListings(String q, String category, String city)
            throws SQLException {

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT l.listing_id, l.title, o.org_name, loc.city, l.status, ")
           .append("COALESCE(SUM(li.quantity), 0) AS total_quantity, ")
           .append("MIN(li.expiry_date) AS earliest_expiry, ")
           .append("GROUP_CONCAT(DISTINCT fc.category_name ORDER BY fc.category_name SEPARATOR ', ') AS categories ")
           .append("FROM listings l ")
           .append("JOIN organizations o ON l.org_id = o.org_id ")
           .append("JOIN location loc ON l.location_id = loc.location_id ")
           .append("LEFT JOIN listingitems li ON l.listing_id = li.listing_id ")
           .append("LEFT JOIN foodcategories fc ON li.category_id = fc.category_id ")
           .append("WHERE l.status = 'OPEN' ");

        List<Object> params = new ArrayList<>();

        if (q != null && !q.isEmpty()) {
            sql.append("AND (l.title LIKE ? OR o.org_name LIKE ?) ");
            String like = "%" + q + "%";
            params.add(like);
            params.add(like);
        }

        if (category != null && !category.isEmpty()) {
            sql.append("AND fc.category_name = ? ");
            params.add(category);
        }

        if (city != null && !city.isEmpty()) {
            sql.append("AND loc.city = ? ");
            params.add(city);
        }

        sql.append("GROUP BY l.listing_id, l.title, o.org_name, loc.city, l.status, l.created_at ")
           .append("ORDER BY earliest_expiry ASC, l.created_at DESC");

        List<ListingRow> rows = new ArrayList<>();

        try (Connection conn = DBConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ListingRow row = new ListingRow();
                    row.listingId = rs.getInt("listing_id");
                    row.title = rs.getString("title");
                    row.orgName = rs.getString("org_name");
                    row.city = rs.getString("city");
                    row.status = rs.getString("status");
                    row.totalQuantity = rs.getInt("total_quantity");
                    row.categories = rs.getString("categories");
                    row.earliestExpiry = rs.getDate("earliest_expiry");
                    rows.add(row);
                }
            }
        }

        return rows;
    }

    private static String trim(String s) {
        return s == null ? null : s.trim();
    }
}
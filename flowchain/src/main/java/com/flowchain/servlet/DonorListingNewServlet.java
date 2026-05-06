package com.flowchain.servlet;

import com.flowchain.db.DBConnection;
import com.flowchain.util.CsrfUtil;

import java.io.IOException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    private static class ItemInput {
        int categoryId;
        int quantity;
        String unit;
        LocalDate expiryDate;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        Integer orgId = getSessionOrgId(req.getSession(false));
        if (orgId == null) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        renderForm(req, res, orgId);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        if (!CsrfUtil.isValid(req)) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid CSRF token");
            return;
        }

        HttpSession session = req.getSession(false);
        Integer orgId = getSessionOrgId(session);
        Integer userId = getSessionUserId(session);

        if (orgId == null || userId == null) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String title = trim(req.getParameter("title"));
        String description = trim(req.getParameter("description"));
        Integer locationId = parseInt(req.getParameter("locationId"));
        List<ItemInput> items = parseItems(req);

        if (title == null || title.isEmpty()) {
            req.setAttribute("error", "Listing title is required.");
            renderForm(req, res, orgId);
            return;
        }

        if (locationId == null) {
            req.setAttribute("error", "Pickup location is required.");
            renderForm(req, res, orgId);
            return;
        }

        if (items.isEmpty()) {
            req.setAttribute("error", "At least one complete food item is required.");
            renderForm(req, res, orgId);
            return;
        }

        try {
            int listingId = createListing(userId, orgId, locationId, title, description, items);
            res.sendRedirect(req.getContextPath() + "/donor/listings/detail?id=" + listingId);
        } catch (IllegalArgumentException e) {
            req.setAttribute("error", e.getMessage());
            renderForm(req, res, orgId);
        } catch (SQLException e) {
            getServletContext().log("Failed to create donor listing", e);
            req.setAttribute("error", "Could not create listing. Please check your entries and try again.");
            renderForm(req, res, orgId);
        }
    }

    private void renderForm(HttpServletRequest req, HttpServletResponse res, int orgId)
            throws ServletException, IOException {
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

    private static int createListing(int userId, int orgId, int locationId, String title,
                                     String description, List<ItemInput> items)
            throws SQLException {

        try (Connection conn = DBConnection.get()) {
            conn.setAutoCommit(false);

            try {
                if (!locationBelongsToOrg(conn, locationId, orgId)) {
                    throw new IllegalArgumentException("Selected pickup location does not belong to your organization.");
                }

                int listingId = nextId(conn, "listings", "listing_id");

                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO listings (listing_id, org_id, location_id, title, description, created_at, status) "
                      + "VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                    ps.setInt(1, listingId);
                    ps.setInt(2, orgId);
                    ps.setInt(3, locationId);
                    ps.setString(4, title);
                    ps.setString(5, description);
                    ps.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
                    ps.setString(7, "OPEN");
                    ps.executeUpdate();
                }

                int nextItemId = nextId(conn, "listingitems", "listing_item_id");
                for (ItemInput item : items) {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO listingitems "
                          + "(listing_item_id, listing_id, category_id, quantity, unit, expiry_date) "
                          + "VALUES (?, ?, ?, ?, ?, ?)")) {
                        ps.setInt(1, nextItemId++);
                        ps.setInt(2, listingId);
                        ps.setInt(3, item.categoryId);
                        ps.setInt(4, item.quantity);
                        ps.setString(5, item.unit);
                        ps.setDate(6, Date.valueOf(item.expiryDate));
                        ps.executeUpdate();
                    }
                }

                writeAuditLog(conn, userId, "CREATE", "LISTING", listingId);

                conn.commit();
                return listingId;
            } catch (SQLException | IllegalArgumentException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    private static boolean locationBelongsToOrg(Connection conn, int locationId, int orgId)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM location WHERE location_id = ? AND org_id = ?")) {
            ps.setInt(1, locationId);
            ps.setInt(2, orgId);

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) == 1;
            }
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

    private static List<ItemInput> parseItems(HttpServletRequest req) {
        String[] categoryIds = req.getParameterValues("categoryId");
        String[] quantities = req.getParameterValues("quantity");
        String[] units = req.getParameterValues("unit");
        String[] expiryDates = req.getParameterValues("expiryDate");

        List<ItemInput> items = new ArrayList<>();

        if (categoryIds == null || quantities == null || units == null || expiryDates == null) {
            return items;
        }

        int count = Math.min(Math.min(categoryIds.length, quantities.length),
                Math.min(units.length, expiryDates.length));

        for (int i = 0; i < count; i++) {
            String categoryText = trim(categoryIds[i]);
            String quantityText = trim(quantities[i]);
            String unitText = trim(units[i]);
            String expiryText = trim(expiryDates[i]);

            boolean allBlank = isBlank(categoryText)
                    && isBlank(quantityText)
                    && isBlank(unitText)
                    && isBlank(expiryText);

            if (allBlank) {
                continue;
            }

            Integer categoryId = parseInt(categoryText);
            Integer quantity = parseInt(quantityText);
            LocalDate expiryDate = parseDate(expiryText);

            if (categoryId == null || quantity == null || quantity <= 0
                    || isBlank(unitText) || expiryDate == null) {
                continue;
            }

            if (expiryDate.isBefore(LocalDate.now())) {
                continue;
            }

            ItemInput item = new ItemInput();
            item.categoryId = categoryId;
            item.quantity = quantity;
            item.unit = unitText;
            item.expiryDate = expiryDate;
            items.add(item);
        }

        return items;
    }

    private static int nextId(Connection conn, String table, String idColumn) throws SQLException {
        String sql = "SELECT COALESCE(MAX(" + idColumn + "), 0) + 1 FROM " + table;
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private static void writeAuditLog(Connection conn, int userId, String actionType, String entityType, int entityId)
            throws SQLException {
        int logId = nextId(conn, "auditlogs", "log_id");

        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO auditlogs (log_id, user_id, action_type, entity_type, entity_id, action_time) "
              + "VALUES (?, ?, ?, ?, ?, ?)")) {
            ps.setInt(1, logId);
            ps.setInt(2, userId);
            ps.setString(3, actionType);
            ps.setString(4, entityType);
            ps.setInt(5, entityId);
            ps.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        }
    }

    private static Integer getSessionOrgId(HttpSession session) {
        if (session == null) return null;
        Object value = session.getAttribute("orgId");
        return (value instanceof Integer) ? (Integer) value : null;
    }

    private static Integer getSessionUserId(HttpSession session) {
        if (session == null) return null;
        Object value = session.getAttribute("userId");
        return (value instanceof Integer) ? (Integer) value : null;
    }

    private static Integer parseInt(String s) {
        try {
            return (s == null || s.trim().isEmpty()) ? null : Integer.valueOf(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static LocalDate parseDate(String s) {
        try {
            return (s == null || s.trim().isEmpty()) ? null : LocalDate.parse(s.trim());
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String trim(String s) {
        return s == null ? null : s.trim();
    }
}

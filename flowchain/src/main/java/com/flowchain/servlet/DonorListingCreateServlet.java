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
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet("/donor/listings/new")
public class DonorListingCreateServlet extends HttpServlet {

    private static final Pattern POSITIVE_INTEGER = Pattern.compile("^[1-9][0-9]*$");
    private static final int ITEM_ROW_COUNT = 4;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        Integer orgId = getSessionOrgId(req);
        if (orgId == null) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN, "You are not authorized to view this page.");
            return;
        }

        try {
            Form form = new Form();
            form.ensureItemRows(ITEM_ROW_COUNT);
            req.setAttribute("form", form);
            req.setAttribute("locations", loadLocations(orgId));
            req.setAttribute("categories", loadCategories());
            req.setAttribute("csrfToken", CsrfUtil.getOrCreate(req));
            req.getRequestDispatcher("/WEB-INF/views/donor-listing-form.jsp").forward(req, res);
        } catch (SQLException e) {
            getServletContext().log("Failed to load listing form", e);
            res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Could not load the listing form.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        if (!CsrfUtil.isValid(req)) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid CSRF token");
            return;
        }

        Integer orgId = getSessionOrgId(req);
        Integer userId = getSessionUserId(req);
        if (orgId == null || userId == null) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN, "You are not authorized to perform this action.");
            return;
        }

        Form form = Form.from(req);
        Map<String, String> errors = form.validate();

        Integer locationId = null;
        if (errors.isEmpty()) {
            try {
                locationId = Integer.parseInt(form.locationId);
            } catch (NumberFormatException ignored) {
                errors.put("locationId", "Select a valid location.");
            }
        }

        try {
            if (errors.isEmpty() && !isLocationOwnedByOrg(locationId, orgId)) {
                errors.put("locationId", "Selected location is not valid for your organization.");
            }
        } catch (SQLException e) {
            getServletContext().log("Failed to validate location ownership", e);
            res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Could not validate the selected location.");
            return;
        }

        if (!errors.isEmpty()) {
            renderFormWithErrors(req, res, form, errors, orgId);
            return;
        }

        try {
            int listingId = createListing(orgId, userId, form);
            res.sendRedirect(req.getContextPath() + "/donor/listings");
        } catch (SQLException e) {
            getServletContext().log("Failed to create listing", e);
            errors.put("_global", "Could not create the listing. Please try again.");
            renderFormWithErrors(req, res, form, errors, orgId);
        }
    }

    private void renderFormWithErrors(HttpServletRequest req, HttpServletResponse res,
                                      Form form, Map<String, String> errors, int orgId)
            throws ServletException, IOException {
        try {
            req.setAttribute("form", form);
            req.setAttribute("locations", loadLocations(orgId));
            req.setAttribute("categories", loadCategories());
            req.setAttribute("errors", errors);
            req.setAttribute("csrfToken", CsrfUtil.getOrCreate(req));
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            req.getRequestDispatcher("/WEB-INF/views/donor-listing-form.jsp").forward(req, res);
        } catch (SQLException e) {
            getServletContext().log("Failed to render listing form with errors", e);
            res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Could not render the listing form.");
        }
    }

    private int createListing(int orgId, int userId, Form form) throws SQLException {
        try (Connection conn = DBConnection.get()) {
            conn.setAutoCommit(false);
            try {
                int listingId = nextId(conn, "listings", "listing_id");
                int itemId = nextId(conn, "listingitems", "listing_item_id");

                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO listings (listing_id, org_id, location_id, title, description, created_at, status) "
                      + "VALUES (?, ?, ?, ?, ?, ?, 'OPEN')")) {
                    ps.setInt(1, listingId);
                    ps.setInt(2, orgId);
                    ps.setInt(3, Integer.parseInt(form.locationId));
                    ps.setString(4, form.title);
                    ps.setString(5, form.description);
                    ps.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO listingitems (listing_item_id, listing_id, category_id, quantity, unit, expiry_date) "
                      + "VALUES (?, ?, ?, ?, ?, ? )")) {
                    for (Form.ItemRow item : form.getValidItems()) {
                        ps.setInt(1, itemId++);
                        ps.setInt(2, listingId);
                        ps.setInt(3, Integer.parseInt(item.categoryId));
                        ps.setInt(4, Integer.parseInt(item.quantity));
                        ps.setString(5, item.unit);
                        ps.setDate(6, Date.valueOf(LocalDate.parse(item.expiryDate)));
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                int logId = nextId(conn, "auditlogs", "log_id");
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO auditlogs (log_id, user_id, action_type, entity_type, entity_id, action_time) "
                      + "VALUES (?, ?, ?, ?, ?, ?)")) {
                    ps.setInt(1, logId);
                    ps.setInt(2, userId);
                    ps.setString(3, "CREATE");
                    ps.setString(4, "LISTING");
                    ps.setInt(5, listingId);
                    ps.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
                    ps.executeUpdate();
                }

                conn.commit();
                return listingId;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    private static boolean isLocationOwnedByOrg(Integer locationId, int orgId) throws SQLException {
        if (locationId == null) return false;
        String sql = "SELECT 1 FROM location WHERE location_id = ? AND org_id = ? LIMIT 1";
        try (Connection conn = DBConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, locationId);
            ps.setInt(2, orgId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static List<LocationRow> loadLocations(int orgId) throws SQLException {
        String sql = "SELECT location_id, address, city, zip FROM location WHERE org_id = ? ORDER BY city, address";
        try (Connection conn = DBConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orgId);
            try (ResultSet rs = ps.executeQuery()) {
                List<LocationRow> rows = new ArrayList<>();
                while (rs.next()) {
                    LocationRow row = new LocationRow();
                    row.locationId = rs.getInt("location_id");
                    row.address = rs.getString("address");
                    row.city = rs.getString("city");
                    row.zip = rs.getString("zip");
                    rows.add(row);
                }
                return rows;
            }
        }
    }

    private static List<CategoryRow> loadCategories() throws SQLException {
        String sql = "SELECT category_id, category_name FROM foodcategories ORDER BY category_name";
        try (Connection conn = DBConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<CategoryRow> rows = new ArrayList<>();
            while (rs.next()) {
                CategoryRow row = new CategoryRow();
                row.categoryId = rs.getInt("category_id");
                row.categoryName = rs.getString("category_name");
                rows.add(row);
            }
            return rows;
        }
    }

    private static int nextId(Connection conn, String table, String pkColumn) throws SQLException {
        String sql = "SELECT COALESCE(MAX(" + pkColumn + "), 0) + 1 FROM " + table;
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private static Integer getSessionOrgId(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) return null;
        return (Integer) session.getAttribute("orgId");
    }

    private static Integer getSessionUserId(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) return null;
        return (Integer) session.getAttribute("userId");
    }

    private static final class LocationRow {
        int locationId;
        String address;
        String city;
        String zip;
    }

    private static final class CategoryRow {
        int categoryId;
        String categoryName;
    }

    private static final class Form {
        String title;
        String description;
        String locationId;
        List<ItemRow> itemRows = new ArrayList<>();

        static Form from(HttpServletRequest req) {
            Form form = new Form();
            form.title = trim(req.getParameter("title"));
            form.description = trim(req.getParameter("description"));
            form.locationId = trim(req.getParameter("locationId"));

            String[] categories = req.getParameterValues("itemCategory");
            String[] quantities = req.getParameterValues("itemQuantity");
            String[] units = req.getParameterValues("itemUnit");
            String[] expiries = req.getParameterValues("itemExpiry");

            int rows = Math.max(ITEM_ROW_COUNT, Math.max(
                    categories == null ? 0 : categories.length,
                    Math.max(quantities == null ? 0 : quantities.length,
                             Math.max(units == null ? 0 : units.length,
                                      expiries == null ? 0 : expiries.length))));

            for (int i = 0; i < rows; i++) {
                ItemRow item = new ItemRow();
                item.categoryId = trim(valueAt(categories, i));
                item.quantity = trim(valueAt(quantities, i));
                item.unit = trim(valueAt(units, i));
                item.expiryDate = trim(valueAt(expiries, i));
                form.itemRows.add(item);
            }
            form.ensureItemRows(ITEM_ROW_COUNT);
            return form;
        }

        Map<String, String> validate() {
            Map<String, String> errors = new LinkedHashMap<>();
            if (isBlank(title)) {
                errors.put("title", "Listing title is required.");
            }
            if (isBlank(locationId)) {
                errors.put("locationId", "Location is required.");
            } else if (!POSITIVE_INTEGER.matcher(locationId).matches()) {
                errors.put("locationId", "Select a valid location.");
            }

            int validItems = 0;
            for (int i = 0; i < itemRows.size(); i++) {
                ItemRow item = itemRows.get(i);
                if (item.isEmpty()) {
                    continue;
                }
                validItems++;
                if (isBlank(item.categoryId) || isBlank(item.quantity)
                        || isBlank(item.unit) || isBlank(item.expiryDate)) {
                    errors.put("item" + i, "Each item must include category, quantity, unit, and expiry date.");
                    continue;
                }
                if (!POSITIVE_INTEGER.matcher(item.categoryId).matches()) {
                    errors.put("item" + i, "Select a valid category.");
                }
                if (!POSITIVE_INTEGER.matcher(item.quantity).matches()) {
                    errors.put("item" + i, "Quantity must be a positive number.");
                }
                try {
                    LocalDate.parse(item.expiryDate);
                } catch (DateTimeParseException e) {
                    errors.put("item" + i, "Expiry date must be in YYYY-MM-DD format.");
                }
            }

            if (validItems == 0) {
                errors.put("items", "At least one item is required.");
            }

            return errors;
        }

        List<ItemRow> getValidItems() {
            List<ItemRow> validItems = new ArrayList<>();
            for (ItemRow item : itemRows) {
                if (!item.isEmpty()) {
                    validItems.add(item);
                }
            }
            return validItems;
        }

        void ensureItemRows(int count) {
            while (itemRows.size() < count) {
                itemRows.add(new ItemRow());
            }
        }

        private static String valueAt(String[] values, int index) {
            return (values != null && index < values.length) ? values[index] : null;
        }

        private static String trim(String s) {
            return s == null ? null : s.trim();
        }

        private static boolean isBlank(String s) {
            return s == null || s.isEmpty();
        }
    }

    private static final class ItemRow {
        String categoryId;
        String quantity;
        String unit;
        String expiryDate;

        boolean isEmpty() {
            return (categoryId == null || categoryId.isEmpty())
                && (quantity == null || quantity.isEmpty())
                && (unit == null || unit.isEmpty())
                && (expiryDate == null || expiryDate.isEmpty());
        }

        public String getCategoryId() {
            return categoryId;
        }

        public String getQuantity() {
            return quantity;
        }

        public String getUnit() {
            return unit;
        }

        public String getExpiryDate() {
            return expiryDate;
        }
    }
}

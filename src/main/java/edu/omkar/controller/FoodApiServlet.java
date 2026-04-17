package edu.omkar.controller;

import com.google.gson.Gson;
import edu.omkar.dbconfig.DBConfig;
import edu.omkar.model.UserModel;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/api/*")
@MultipartConfig
public class FoodApiServlet extends HttpServlet {
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String path = getPath(request);
        try {
            if ("/session".equals(path)) {
                writeJson(response, getSessionUser(request));
            } else if ("/stats".equals(path)) {
                writeJson(response, getStats());
            } else if ("/restaurants".equals(path)) {
                writeJson(response, getRestaurants());
            } else if ("/menu".equals(path)) {
                writeJson(response, getMenu(request.getParameter("restaurantId")));
            } else if ("/orders".equals(path)) {
                writeJson(response, getOrders(request));
            } else if ("/users".equals(path)) {
                writeJson(response, getUsers());
            } else {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Unknown API endpoint");
            }
        } catch (IllegalArgumentException ex) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
        } catch (Exception ex) {
            ex.printStackTrace();
            writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ex.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String path = getPath(request);
        try {
            if ("/restaurants".equals(path)) {
                writeJson(response, addRestaurant(request));
            } else if ("/restaurants/update".equals(path)) {
                writeJson(response, updateRestaurant(request));
            } else if ("/restaurants/delete".equals(path)) {
                writeJson(response, deleteById("restaurants", request.getParameter("id")));
            } else if ("/menu".equals(path)) {
                writeJson(response, addMenuItem(request));
            } else if ("/menu/update".equals(path)) {
                writeJson(response, updateMenuItem(request));
            } else if ("/menu/delete".equals(path)) {
                writeJson(response, deleteById("food_items", request.getParameter("id")));
            } else if ("/orders/place".equals(path)) {
                writeJson(response, placeOrder(request));
            } else if ("/orders/status".equals(path)) {
                writeJson(response, updateOrderStatus(request));
            } else if ("/logout".equals(path)) {
                HttpSession session = request.getSession(false);
                if (session != null) {
                    session.invalidate();
                }
                writeJson(response, ok("Logged out"));
            } else {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Unknown API endpoint");
            }
        } catch (IllegalArgumentException ex) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
        } catch (Exception ex) {
            ex.printStackTrace();
            writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ex.getMessage());
        }
    }

    private String getPath(HttpServletRequest request) {
        String path = request.getPathInfo();
        return path == null || path.trim().isEmpty() ? "/" : path;
    }

    private Object getSessionUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? null : session.getAttribute("user");
    }

    private Map<String, Object> getStats() throws Exception {
        Map<String, Object> stats = new HashMap<>();
        try (Connection conn = DBConfig.getConnection()) {
            stats.put("restaurants", scalar(conn, "SELECT COUNT(*) FROM restaurants"));
            stats.put("menuItems", scalar(conn, "SELECT COUNT(*) FROM food_items"));
            stats.put("orders", scalar(conn, "SELECT COUNT(*) FROM orders"));
            stats.put("customers", scalar(conn, "SELECT COUNT(*) FROM users WHERE role = 'customer'"));
            stats.put("revenue", scalar(conn, "SELECT COALESCE(SUM(total_amount), 0) FROM orders WHERE status <> 'cancelled'"));
            stats.put("pendingOrders", scalar(conn, "SELECT COUNT(*) FROM orders WHERE status IN ('pending','preparing','out_for_delivery')"));
        }
        return stats;
    }

    private Object scalar(Connection conn, String sql) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getObject(1) : 0;
        }
    }

    private List<Map<String, Object>> getRestaurants() throws Exception {
        List<Map<String, Object>> rows = new ArrayList<>();
        String sql = "SELECT r.*, COUNT(f.id) AS item_count FROM restaurants r "
                + "LEFT JOIN food_items f ON f.restaurant_id = r.id GROUP BY r.id ORDER BY r.id DESC";
        try (Connection conn = DBConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("id", rs.getInt("id"));
                row.put("name", rs.getString("name"));
                row.put("address", rs.getString("address"));
                row.put("contact", rs.getString("contact"));
                row.put("cuisines", rs.getString("cuisines"));
                row.put("rating", rs.getBigDecimal("rating"));
                row.put("deliveryTime", rs.getString("delivery_time"));
                row.put("status", rs.getString("status"));
                row.put("itemCount", rs.getInt("item_count"));
                rows.add(row);
            }
        }
        return rows;
    }

    private List<Map<String, Object>> getMenu(String restaurantId) throws Exception {
        List<Map<String, Object>> rows = new ArrayList<>();
        String sql = "SELECT f.*, r.name AS restaurant_name FROM food_items f JOIN restaurants r ON r.id = f.restaurant_id ";
        if (restaurantId != null && !restaurantId.trim().isEmpty()) {
            sql += "WHERE f.restaurant_id = ? ";
        }
        sql += "ORDER BY r.name, f.category, f.name";

        try (Connection conn = DBConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (restaurantId != null && !restaurantId.trim().isEmpty()) {
                ps.setInt(1, Integer.parseInt(restaurantId));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("id", rs.getInt("id"));
                    row.put("restaurantId", rs.getInt("restaurant_id"));
                    row.put("restaurantName", rs.getString("restaurant_name"));
                    row.put("name", rs.getString("name"));
                    row.put("description", rs.getString("description"));
                    row.put("imageUrl", rs.getString("image_url"));
                    row.put("price", rs.getBigDecimal("price"));
                    row.put("category", rs.getString("category"));
                    row.put("veg", rs.getBoolean("veg"));
                    row.put("available", rs.getBoolean("available"));
                    rows.add(row);
                }
            }
        }
        return rows;
    }

    private List<Map<String, Object>> getOrders(HttpServletRequest request) throws Exception {
        UserModel user = (UserModel) getSessionUser(request);
        String userIdParam = request.getParameter("userId");
        boolean ownOrders = userIdParam != null && !userIdParam.trim().isEmpty();
        boolean customerSession = user != null && "customer".equalsIgnoreCase(user.getRole());

        String sql = "SELECT o.*, u.email FROM orders o JOIN users u ON u.id = o.user_id ";
        if (ownOrders || customerSession) {
            sql += "WHERE o.user_id = ? ";
        }
        sql += "ORDER BY o.order_date DESC";

        List<Map<String, Object>> orders = new ArrayList<>();
        try (Connection conn = DBConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (ownOrders) {
                ps.setInt(1, Integer.parseInt(userIdParam));
            } else if (customerSession) {
                ps.setInt(1, user.getId());
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> order = new HashMap<>();
                    int orderId = rs.getInt("id");
                    order.put("id", orderId);
                    order.put("userId", rs.getInt("user_id"));
                    order.put("customerName", rs.getString("customer_name"));
                    order.put("customerPhone", rs.getString("customer_phone"));
                    order.put("customerEmail", rs.getString("email"));
                    order.put("deliveryAddress", rs.getString("delivery_address"));
                    order.put("totalAmount", rs.getBigDecimal("total_amount"));
                    order.put("status", rs.getString("status"));
                    order.put("paymentMethod", rs.getString("payment_method"));
                    order.put("orderDate", String.valueOf(rs.getTimestamp("order_date")));
                    order.put("items", getOrderItems(conn, orderId));
                    orders.add(order);
                }
            }
        }
        return orders;
    }

    private List<Map<String, Object>> getOrderItems(Connection conn, int orderId) throws Exception {
        List<Map<String, Object>> items = new ArrayList<>();
        String sql = "SELECT * FROM order_items WHERE order_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", rs.getInt("id"));
                    item.put("restaurantName", rs.getString("restaurant_name"));
                    item.put("itemName", rs.getString("item_name"));
                    item.put("quantity", rs.getInt("quantity"));
                    item.put("price", rs.getBigDecimal("price"));
                    items.add(item);
                }
            }
        }
        return items;
    }

    private List<Map<String, Object>> getUsers() throws Exception {
        List<Map<String, Object>> rows = new ArrayList<>();
        String sql = "SELECT id, name, email, phone, address, role, created_at FROM users ORDER BY created_at DESC";
        try (Connection conn = DBConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("id", rs.getInt("id"));
                row.put("name", rs.getString("name"));
                row.put("email", rs.getString("email"));
                row.put("phone", rs.getString("phone"));
                row.put("address", rs.getString("address"));
                row.put("role", rs.getString("role"));
                row.put("createdAt", String.valueOf(rs.getTimestamp("created_at")));
                rows.add(row);
            }
        }
        return rows;
    }

    private Map<String, Object> addRestaurant(HttpServletRequest request) throws Exception {
        String name = required(request, "name", "Restaurant name is required");
        String address = required(request, "address", "Restaurant address is required");
        String contact = required(request, "contact", "Restaurant contact is required");
        String cuisines = required(request, "cuisines", "Restaurant cuisines are required");
        String sql = "INSERT INTO restaurants (name, address, contact, cuisines, rating, delivery_time, status) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConfig.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, address);
            ps.setString(3, contact);
            ps.setString(4, cuisines);
            ps.setBigDecimal(5, decimalParam(request, "rating", "4.2"));
            ps.setString(6, valueOrDefault(request.getParameter("deliveryTime"), "30-40 min"));
            ps.setString(7, valueOrDefault(request.getParameter("status"), "open"));
            ps.executeUpdate();
        }
        return ok("Restaurant added");
    }

    private Map<String, Object> updateRestaurant(HttpServletRequest request) throws Exception {
        int id = requiredInt(request, "id", "Restaurant id is required");
        String name = required(request, "name", "Restaurant name is required");
        String address = required(request, "address", "Restaurant address is required");
        String contact = required(request, "contact", "Restaurant contact is required");
        String cuisines = required(request, "cuisines", "Restaurant cuisines are required");
        String sql = "UPDATE restaurants SET name=?, address=?, contact=?, cuisines=?, rating=?, delivery_time=?, status=? WHERE id=?";
        try (Connection conn = DBConfig.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, address);
            ps.setString(3, contact);
            ps.setString(4, cuisines);
            ps.setBigDecimal(5, decimalParam(request, "rating", "4.2"));
            ps.setString(6, valueOrDefault(request.getParameter("deliveryTime"), "30-40 min"));
            ps.setString(7, valueOrDefault(request.getParameter("status"), "open"));
            ps.setInt(8, id);
            ps.executeUpdate();
        }
        return ok("Restaurant updated");
    }

    private Map<String, Object> addMenuItem(HttpServletRequest request) throws Exception {
        String name = required(request, "name", "Menu item name is required");
        String restaurantId = required(request, "restaurantId", "Restaurant selection is required");
        String sql = "INSERT INTO food_items (restaurant_id, name, description, image_url, price, category, veg, available) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConfig.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, Integer.parseInt(restaurantId));
            ps.setString(2, name);
            ps.setString(3, request.getParameter("description"));
            ps.setString(4, valueOrDefault(request.getParameter("imageUrl"), defaultMenuImage()));
            ps.setBigDecimal(5, decimalParam(request, "price", "0"));
            ps.setString(6, request.getParameter("category"));
            ps.setBoolean(7, "true".equalsIgnoreCase(request.getParameter("veg")) || "on".equalsIgnoreCase(request.getParameter("veg")));
            ps.setBoolean(8, !"false".equalsIgnoreCase(request.getParameter("available")));
            ps.executeUpdate();
        }
        return ok("Menu item added");
    }

    private Map<String, Object> updateMenuItem(HttpServletRequest request) throws Exception {
        int id = requiredInt(request, "id", "Menu item id is required");
        String name = required(request, "name", "Menu item name is required");
        String restaurantId = required(request, "restaurantId", "Restaurant selection is required");
        String sql = "UPDATE food_items SET restaurant_id=?, name=?, description=?, image_url=?, price=?, category=?, veg=?, available=? WHERE id=?";
        try (Connection conn = DBConfig.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, Integer.parseInt(restaurantId));
            ps.setString(2, name);
            ps.setString(3, request.getParameter("description"));
            ps.setString(4, valueOrDefault(request.getParameter("imageUrl"), defaultMenuImage()));
            ps.setBigDecimal(5, decimalParam(request, "price", "0"));
            ps.setString(6, request.getParameter("category"));
            ps.setBoolean(7, "true".equalsIgnoreCase(request.getParameter("veg")) || "on".equalsIgnoreCase(request.getParameter("veg")));
            ps.setBoolean(8, !"false".equalsIgnoreCase(request.getParameter("available")));
            ps.setInt(9, id);
            ps.executeUpdate();
        }
        return ok("Menu item updated");
    }

    private Map<String, Object> placeOrder(HttpServletRequest request) throws Exception {
        UserModel user = (UserModel) getSessionUser(request);
        if (user == null) {
            throw new IllegalStateException("Please login before placing an order");
        }

        OrderRequest order = gson.fromJson(request.getReader(), OrderRequest.class);
        if (order == null || order.items == null || order.items.isEmpty()) {
            throw new IllegalArgumentException("Cart is empty");
        }

        try (Connection conn = DBConfig.getConnection()) {
            conn.setAutoCommit(false);
            try {
                BigDecimal total = BigDecimal.ZERO;
                List<OrderItemResolved> resolvedItems = new ArrayList<>();
                for (OrderLine line : order.items) {
                    OrderItemResolved resolved = resolveItem(conn, line.id, line.quantity);
                    total = total.add(resolved.price.multiply(BigDecimal.valueOf(resolved.quantity)));
                    resolvedItems.add(resolved);
                }

                String orderSql = "INSERT INTO orders (user_id, customer_name, customer_phone, delivery_address, total_amount, status, payment_method) VALUES (?, ?, ?, ?, ?, 'pending', ?)";
                int orderId;
                try (PreparedStatement ps = conn.prepareStatement(orderSql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, user.getId());
                    ps.setString(2, valueOrDefault(order.customerName, user.getName()));
                    ps.setString(3, valueOrDefault(order.customerPhone, "0000000000"));
                    ps.setString(4, valueOrDefault(order.deliveryAddress, "Address not provided"));
                    ps.setBigDecimal(5, total);
                    ps.setString(6, valueOrDefault(order.paymentMethod, "Cash on Delivery"));
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        keys.next();
                        orderId = keys.getInt(1);
                    }
                }

                String itemSql = "INSERT INTO order_items (order_id, food_item_id, restaurant_name, item_name, quantity, price) VALUES (?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(itemSql)) {
                    for (OrderItemResolved item : resolvedItems) {
                        ps.setInt(1, orderId);
                        ps.setInt(2, item.id);
                        ps.setString(3, item.restaurantName);
                        ps.setString(4, item.name);
                        ps.setInt(5, item.quantity);
                        ps.setBigDecimal(6, item.price);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                conn.commit();
                Map<String, Object> result = ok("Order placed successfully");
                result.put("orderId", orderId);
                result.put("total", total);
                return result;
            } catch (Exception ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    private OrderItemResolved resolveItem(Connection conn, int id, int quantity) throws Exception {
        String sql = "SELECT f.id, f.name, f.price, r.name AS restaurant_name FROM food_items f JOIN restaurants r ON r.id = f.restaurant_id WHERE f.id = ? AND f.available = TRUE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalArgumentException("Unavailable food item id: " + id);
                }
                OrderItemResolved item = new OrderItemResolved();
                item.id = rs.getInt("id");
                item.name = rs.getString("name");
                item.restaurantName = rs.getString("restaurant_name");
                item.price = rs.getBigDecimal("price");
                item.quantity = Math.max(1, quantity);
                return item;
            }
        }
    }

    private Map<String, Object> updateOrderStatus(HttpServletRequest request) throws Exception {
        int id = requiredInt(request, "id", "Order id is required");
        String status = required(request, "status", "Order status is required");
        String sql = "UPDATE orders SET status = ? WHERE id = ?";
        try (Connection conn = DBConfig.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
        return ok("Order status updated");
    }

    private Map<String, Object> deleteById(String table, String id) throws Exception {
        if (!"restaurants".equals(table) && !"food_items".equals(table)) {
            throw new IllegalArgumentException("Invalid table");
        }
        try (Connection conn = DBConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM " + table + " WHERE id = ?")) {
            ps.setInt(1, parseRequiredInt(id, "Record id is required"));
            ps.executeUpdate();
        }
        return ok("Deleted");
    }

    private BigDecimal decimalParam(HttpServletRequest request, String name, String fallback) {
        String value = request.getParameter(name);
        if (value == null || value.trim().isEmpty()) {
            value = fallback;
        }
        return new BigDecimal(value);
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private String required(HttpServletRequest request, String name, String message) {
        String value = request.getParameter(name);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private int requiredInt(HttpServletRequest request, String name, String message) {
        return parseRequiredInt(request.getParameter(name), message);
    }

    private int parseRequiredInt(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return Integer.parseInt(value.trim());
    }

    private String defaultMenuImage() {
        return "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?auto=format&fit=crop&w=900&q=80";
    }

    private Map<String, Object> ok(String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", message);
        return result;
    }

    private void writeJson(HttpServletResponse response, Object data) throws IOException {
        response.getWriter().write(gson.toJson(data));
    }

    private void writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("message", valueOrDefault(message, "Request failed"));
        response.getWriter().write(gson.toJson(error));
    }

    private static class OrderRequest {
        String customerName;
        String customerPhone;
        String deliveryAddress;
        String paymentMethod;
        List<OrderLine> items;
    }

    private static class OrderLine {
        int id;
        int quantity;
    }

    private static class OrderItemResolved {
        int id;
        String name;
        String restaurantName;
        BigDecimal price;
        int quantity;
    }
}

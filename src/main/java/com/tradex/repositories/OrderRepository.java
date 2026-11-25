package com.tradex.repositories;

import com.tradex.database.Database;
import com.tradex.exceptions.DataAccessException;
import com.tradex.models.*;

import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

public class OrderRepository {
    private static final String COLS = "id, user_id, symbol, side, type, quantity, limit_price, status, created_at";

    public long insertPending(long userId, String symbol, Side side, OrderType type, int qty, BigDecimal trigger) throws DataAccessException {
        String sql = "INSERT INTO orders (user_id, symbol, side, type, quantity, limit_price, status) VALUES (?, ?, ?, ?, ?, ?, 'PENDING')";
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, userId);
            ps.setString(2, symbol);
            ps.setString(3, side.name());
            ps.setString(4, type.name());
            ps.setInt(5, qty);
            ps.setBigDecimal(6, trigger);
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { k.next(); return k.getLong(1); }
        } catch (SQLException e) { throw SqlSupport.wrap("Failed to place order", e); }
    }

    public Optional<OrderRecord> find(long id) throws DataAccessException {
        return query("SELECT " + COLS + " FROM orders WHERE id = ?", id).stream().findFirst();
    }

    public List<OrderRecord> findByUser(long userId) throws DataAccessException {
        return query("SELECT " + COLS + " FROM orders WHERE user_id = ? ORDER BY id DESC", userId);
    }

    public List<OrderRecord> findAllPending() throws DataAccessException {
        return query("SELECT " + COLS + " FROM orders WHERE status = 'PENDING' ORDER BY id");
    }

    /** Cancels the user's own pending order. Returns false if it is not pending (or not theirs). */
    public boolean cancel(long userId, long orderId) throws DataAccessException {
        String sql = "UPDATE orders SET status = 'CANCELLED' WHERE id = ? AND user_id = ? AND status = 'PENDING'";
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, orderId);
            ps.setLong(2, userId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) { throw SqlSupport.wrap("Failed to cancel order", e); }
    }

    /** Locks the order row and reports whether it is still pending. */
    public boolean lockIfPending(Connection c, long orderId) throws DataAccessException {
        try (PreparedStatement ps = c.prepareStatement("SELECT status FROM orders WHERE id = ? FOR UPDATE")) {
            ps.setLong(1, orderId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() && rs.getString(1).equals("PENDING"); }
        } catch (SQLException e) { throw SqlSupport.wrap("Failed to lock order", e); }
    }

    public void markStatus(Connection c, long orderId, OrderStatus status) throws DataAccessException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE orders SET status = ? WHERE id = ?")) {
            ps.setString(1, status.name());
            ps.setLong(2, orderId);
            ps.executeUpdate();
        } catch (SQLException e) { throw SqlSupport.wrap("Failed to update order", e); }
    }

    private List<OrderRecord> query(String sql, Object... params) throws DataAccessException {
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
            try (ResultSet rs = ps.executeQuery()) {
                List<OrderRecord> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(new OrderRecord(rs.getLong(1), rs.getLong(2), rs.getString(3), Side.valueOf(rs.getString(4)),
                            OrderType.valueOf(rs.getString(5)), rs.getInt(6), rs.getBigDecimal(7),
                            OrderStatus.valueOf(rs.getString(8)), rs.getTimestamp(9).toLocalDateTime()));
                }
                return out;
            }
        } catch (SQLException e) { throw SqlSupport.wrap("Failed to load orders", e); }
    }
}

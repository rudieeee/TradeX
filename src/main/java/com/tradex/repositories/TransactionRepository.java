package com.tradex.repositories;

import com.tradex.database.Database;
import com.tradex.exceptions.DataAccessException;
import com.tradex.models.*;

import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

/** Persists executed orders and their transaction records. */
public class TransactionRepository {
    public long insertExecutedOrder(Connection c, Order o) throws DataAccessException {
        String sql = "INSERT INTO orders (user_id, symbol, side, type, quantity, status) VALUES (?, ?, ?, 'MARKET', ?, 'EXECUTED')";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, o.getUserId());
            ps.setString(2, o.getSymbol());
            ps.setString(3, o.getSide().name());
            ps.setInt(4, o.getQuantity());
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { k.next(); return k.getLong(1); }
        } catch (SQLException e) { throw SqlSupport.wrap("Failed to record order", e); }
    }

    public void insertTransaction(Connection c, long orderId, Order o, BigDecimal price) throws DataAccessException {
        String sql = "INSERT INTO transactions (order_id, user_id, symbol, side, quantity, price) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, orderId);
            ps.setLong(2, o.getUserId());
            ps.setString(3, o.getSymbol());
            ps.setString(4, o.getSide().name());
            ps.setInt(5, o.getQuantity());
            ps.setBigDecimal(6, price);
            ps.executeUpdate();
        } catch (SQLException e) { throw SqlSupport.wrap("Failed to record transaction", e); }
    }

    public List<Transaction> findByUser(long userId) throws DataAccessException {
        String sql = "SELECT id, order_id, user_id, symbol, side, quantity, price, executed_at FROM transactions "
                + "WHERE user_id = ? ORDER BY executed_at DESC, id DESC";
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Transaction> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(new Transaction(rs.getLong(1), rs.getLong(2), rs.getLong(3), rs.getString(4),
                            Side.valueOf(rs.getString(5)), rs.getInt(6), rs.getBigDecimal(7),
                            rs.getTimestamp(8).toLocalDateTime()));
                }
                return out;
            }
        } catch (SQLException e) { throw SqlSupport.wrap("Failed to load transactions", e); }
    }
}

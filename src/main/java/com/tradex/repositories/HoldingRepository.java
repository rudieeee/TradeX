package com.tradex.repositories;

import com.tradex.database.Database;
import com.tradex.exceptions.DataAccessException;
import com.tradex.models.Holding;

import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

public class HoldingRepository {
    public Optional<Holding> lock(Connection c, long userId, String symbol) throws DataAccessException {
        String sql = "SELECT user_id, symbol, quantity, avg_price FROM holdings WHERE user_id = ? AND symbol = ? FOR UPDATE";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, symbol);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) { throw SqlSupport.wrap("Failed to read holding", e); }
    }

    public void insert(Connection c, long userId, String symbol, int qty, BigDecimal avg) throws DataAccessException {
        exec(c, "INSERT INTO holdings (user_id, symbol, quantity, avg_price) VALUES (?, ?, ?, ?)", userId, symbol, qty, avg);
    }

    public void update(Connection c, long userId, String symbol, int qty, BigDecimal avg) throws DataAccessException {
        exec(c, "UPDATE holdings SET quantity = ?, avg_price = ? WHERE user_id = ? AND symbol = ?", qty, avg, userId, symbol);
    }

    public void delete(Connection c, long userId, String symbol) throws DataAccessException {
        exec(c, "DELETE FROM holdings WHERE user_id = ? AND symbol = ?", userId, symbol);
    }

    public List<Holding> findByUser(long userId) throws DataAccessException {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT user_id, symbol, quantity, avg_price FROM holdings WHERE user_id = ? ORDER BY symbol")) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Holding> out = new ArrayList<>();
                while (rs.next()) out.add(map(rs));
                return out;
            }
        } catch (SQLException e) { throw SqlSupport.wrap("Failed to load holdings", e); }
    }

    private Holding map(ResultSet rs) throws SQLException {
        return new Holding(rs.getLong(1), rs.getString(2), rs.getInt(3), rs.getBigDecimal(4));
    }

    private void exec(Connection c, String sql, Object... params) throws DataAccessException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
            ps.executeUpdate();
        } catch (SQLException e) { throw SqlSupport.wrap("Failed to write holding", e); }
    }
}

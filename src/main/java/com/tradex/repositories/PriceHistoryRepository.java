package com.tradex.repositories;

import com.tradex.database.Database;
import com.tradex.exceptions.DataAccessException;
import com.tradex.models.PricePoint;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class PriceHistoryRepository {
    public int count() throws DataAccessException {
        try (Connection c = Database.getConnection(); Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM price_history")) {
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) { throw SqlSupport.wrap("Failed to count history", e); }
    }

    public void insertBatch(List<Object[]> rows) throws DataAccessException {
        String sql = "INSERT INTO price_history (symbol, price, recorded_at) VALUES (?, ?, ?)";
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            c.setAutoCommit(false);
            for (Object[] r : rows) {
                ps.setString(1, (String) r[0]);
                ps.setBigDecimal(2, (BigDecimal) r[1]);
                ps.setTimestamp(3, Timestamp.valueOf((LocalDateTime) r[2]));
                ps.addBatch();
            }
            ps.executeBatch();
            c.commit();
        } catch (SQLException e) { throw SqlSupport.wrap("Failed to save history", e); }
    }

    /** Most recent recorded price per symbol strictly before {@code cutoff} (i.e. the previous close). */
    public Map<String, BigDecimal> priceBefore(LocalDateTime cutoff) throws DataAccessException {
        String sql = "SELECT ph.symbol, ph.price FROM price_history ph JOIN ("
                + "SELECT symbol, MAX(recorded_at) m FROM price_history WHERE recorded_at < ? GROUP BY symbol) x "
                + "ON ph.symbol = x.symbol AND ph.recorded_at = x.m";
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(cutoff));
            try (ResultSet rs = ps.executeQuery()) {
                Map<String, BigDecimal> out = new HashMap<>();
                while (rs.next()) out.put(rs.getString(1), rs.getBigDecimal(2));
                return out;
            }
        } catch (SQLException e) { throw SqlSupport.wrap("Failed to load previous closes", e); }
    }

    public List<PricePoint> history(String symbol, int limit) throws DataAccessException {
        String sql = "SELECT recorded_at, price FROM (SELECT recorded_at, price FROM price_history WHERE symbol = ? "
                + "ORDER BY recorded_at DESC LIMIT ?) t ORDER BY recorded_at";
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, symbol);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                List<PricePoint> out = new ArrayList<>();
                while (rs.next()) out.add(new PricePoint(rs.getTimestamp(1).toLocalDateTime(), rs.getBigDecimal(2)));
                return out;
            }
        } catch (SQLException e) { throw SqlSupport.wrap("Failed to load history", e); }
    }

    /** Keeps one row per symbol per day: updates today's row if present, otherwise inserts it. */
    public void upsertToday(Map<String, BigDecimal> prices, LocalDateTime now) throws DataAccessException {
        Timestamp dayStart = Timestamp.valueOf(now.toLocalDate().atStartOfDay());
        try (Connection c = Database.getConnection();
             PreparedStatement upd = c.prepareStatement("UPDATE price_history SET price = ?, recorded_at = ? WHERE symbol = ? AND recorded_at >= ?");
             PreparedStatement ins = c.prepareStatement("INSERT INTO price_history (symbol, price, recorded_at) VALUES (?, ?, ?)")) {
            c.setAutoCommit(false);
            for (var e : prices.entrySet()) {
                upd.setBigDecimal(1, e.getValue());
                upd.setTimestamp(2, Timestamp.valueOf(now));
                upd.setString(3, e.getKey());
                upd.setTimestamp(4, dayStart);
                if (upd.executeUpdate() == 0) {
                    ins.setString(1, e.getKey());
                    ins.setBigDecimal(2, e.getValue());
                    ins.setTimestamp(3, Timestamp.valueOf(now));
                    ins.executeUpdate();
                }
            }
            c.commit();
        } catch (SQLException e) { throw SqlSupport.wrap("Failed to record prices", e); }
    }

    public void deleteFor(String symbol) throws DataAccessException {
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement("DELETE FROM price_history WHERE symbol = ?")) {
            ps.setString(1, symbol);
            ps.executeUpdate();
        } catch (SQLException e) { throw SqlSupport.wrap("Failed to clear history", e); }
    }
}

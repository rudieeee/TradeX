package com.tradex.repositories;

import com.tradex.database.Database;
import com.tradex.exceptions.DataAccessException;
import com.tradex.models.PricePoint;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class SnapshotRepository {
    /** Records every user's current total value into the given time bucket (idempotent within a bucket). */
    @SuppressWarnings("deprecation") // VALUES() is deprecated in newer MySQL but still the portable form for INSERT..SELECT
    public void recordAll(LocalDateTime bucket) throws DataAccessException {
        String sql = "INSERT INTO portfolio_snapshots (user_id, bucket, total_value) "
                + "SELECT u.id, ?, w.balance + COALESCE((SELECT SUM(h.quantity * s.current_price) FROM holdings h "
                + "JOIN stocks s ON s.symbol = h.symbol WHERE h.user_id = u.id), 0) "
                + "FROM users u JOIN wallets w ON w.user_id = u.id "
                + "ON DUPLICATE KEY UPDATE total_value = VALUES(total_value)";
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(bucket));
            ps.executeUpdate();
        } catch (SQLException e) { throw SqlSupport.wrap("Failed to record snapshots", e); }
    }

    public List<PricePoint> history(long userId, int limit) throws DataAccessException {
        String sql = "SELECT bucket, total_value FROM (SELECT bucket, total_value FROM portfolio_snapshots "
                + "WHERE user_id = ? ORDER BY bucket DESC LIMIT ?) t ORDER BY bucket";
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                List<PricePoint> out = new ArrayList<>();
                while (rs.next()) out.add(new PricePoint(rs.getTimestamp(1).toLocalDateTime(), rs.getBigDecimal(2)));
                return out;
            }
        } catch (SQLException e) { throw SqlSupport.wrap("Failed to load snapshots", e); }
    }
}

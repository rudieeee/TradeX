package com.tradex.repositories;

import com.tradex.database.Database;
import com.tradex.exceptions.DataAccessException;

import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

public class LeaderboardRepository {
    public record Standing(long userId, String username, BigDecimal totalValue) {}

    /** Total value = cash + holdings at current prices, highest first. Admins are excluded. */
    public List<Standing> standings() throws DataAccessException {
        String sql = "SELECT u.id, u.username, w.balance + COALESCE(("
                + "SELECT SUM(h.quantity * s.current_price) FROM holdings h JOIN stocks s ON s.symbol = h.symbol "
                + "WHERE h.user_id = u.id), 0) AS total "
                + "FROM users u JOIN wallets w ON w.user_id = u.id WHERE u.role = 'USER' ORDER BY total DESC, u.id";
        try (Connection c = Database.getConnection(); Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            List<Standing> out = new ArrayList<>();
            while (rs.next()) out.add(new Standing(rs.getLong(1), rs.getString(2), rs.getBigDecimal(3)));
            return out;
        } catch (SQLException e) { throw SqlSupport.wrap("Failed to load leaderboard", e); }
    }
}

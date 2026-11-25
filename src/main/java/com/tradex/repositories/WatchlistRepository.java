package com.tradex.repositories;

import com.tradex.database.Database;
import com.tradex.exceptions.DataAccessException;

import java.sql.*;
import java.util.*;

public class WatchlistRepository {
    public void add(long userId, String symbol) throws DataAccessException {
        exec("INSERT IGNORE INTO watchlists (user_id, symbol) VALUES (?, ?)", userId, symbol);
    }

    public void remove(long userId, String symbol) throws DataAccessException {
        exec("DELETE FROM watchlists WHERE user_id = ? AND symbol = ?", userId, symbol);
    }

    public Set<String> symbols(long userId) throws DataAccessException {
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement("SELECT symbol FROM watchlists WHERE user_id = ?")) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                Set<String> out = new LinkedHashSet<>();
                while (rs.next()) out.add(rs.getString(1));
                return out;
            }
        } catch (SQLException e) { throw SqlSupport.wrap("Failed to load watchlist", e); }
    }

    private void exec(String sql, long userId, String symbol) throws DataAccessException {
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, symbol);
            ps.executeUpdate();
        } catch (SQLException e) { throw SqlSupport.wrap("Failed to update watchlist", e); }
    }
}

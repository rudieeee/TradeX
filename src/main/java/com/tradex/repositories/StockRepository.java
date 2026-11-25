package com.tradex.repositories;

import com.tradex.database.Database;
import com.tradex.exceptions.DataAccessException;
import com.tradex.models.Stock;

import java.sql.*;
import java.util.*;

public class StockRepository {
    public Optional<Stock> find(Connection c, String symbol) throws DataAccessException {
        try (PreparedStatement ps = c.prepareStatement("SELECT symbol, name, current_price FROM stocks WHERE symbol = ?")) {
            ps.setString(1, symbol);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(new Stock(rs.getString(1), rs.getString(2), rs.getBigDecimal(3))) : Optional.empty();
            }
        } catch (SQLException e) { throw SqlSupport.wrap("Failed to load stock", e); }
    }

    public List<Stock> search(String query) throws DataAccessException {
        String sql = "SELECT symbol, name, current_price FROM stocks WHERE symbol LIKE ? OR name LIKE ? ORDER BY symbol";
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            String like = "%" + (query == null ? "" : query.trim()) + "%";
            ps.setString(1, like);
            ps.setString(2, like);
            try (ResultSet rs = ps.executeQuery()) {
                List<Stock> out = new ArrayList<>();
                while (rs.next()) out.add(new Stock(rs.getString(1), rs.getString(2), rs.getBigDecimal(3)));
                return out;
            }
        } catch (SQLException e) { throw SqlSupport.wrap("Failed to search stocks", e); }
    }
}

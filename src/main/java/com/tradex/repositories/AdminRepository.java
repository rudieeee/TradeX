package com.tradex.repositories;

import com.tradex.database.Database;
import com.tradex.exceptions.DataAccessException;
import com.tradex.models.*;

import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

public class AdminRepository {
    public List<AdminUserRow> listUsers() throws DataAccessException {
        String sql = "SELECT u.id, u.username, u.email, u.role, u.created_at, COALESCE(w.balance, 0), "
                + "(SELECT COUNT(*) FROM transactions t WHERE t.user_id = u.id) "
                + "FROM users u LEFT JOIN wallets w ON w.user_id = u.id ORDER BY u.id";
        try (Connection c = Database.getConnection(); Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            List<AdminUserRow> out = new ArrayList<>();
            while (rs.next()) {
                out.add(new AdminUserRow(rs.getLong(1), rs.getString(2), rs.getString(3), Role.valueOf(rs.getString(4)),
                        rs.getTimestamp(5).toLocalDateTime(), rs.getBigDecimal(6), rs.getInt(7)));
            }
            return out;
        } catch (SQLException e) { throw SqlSupport.wrap("Failed to list users", e); }
    }

    /** Latest trades across all users, as display strings: [time, user, side, qty, symbol, price]. */
    public List<String[]> recentActivity(int limit) throws DataAccessException {
        String sql = "SELECT t.executed_at, u.username, t.side, t.quantity, t.symbol, t.price FROM transactions t "
                + "JOIN users u ON u.id = t.user_id ORDER BY t.executed_at DESC, t.id DESC LIMIT ?";
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                List<String[]> out = new ArrayList<>();
                while (rs.next()) out.add(new String[]{rs.getTimestamp(1).toLocalDateTime().toString().replace('T', ' '),
                        rs.getString(2), rs.getString(3), String.valueOf(rs.getInt(4)), rs.getString(5), rs.getBigDecimal(6).toPlainString()});
                return out;
            }
        } catch (SQLException e) { throw SqlSupport.wrap("Failed to load activity", e); }
    }

    public void deleteUser(long userId) throws DataAccessException {
        inTransaction(c -> { clearActivity(c, userId); update(c, "DELETE FROM users WHERE id = ?", userId); });
    }

    /** Wipes a user's trading data and restores the starting balance. */
    public void resetAccount(long userId) throws DataAccessException {
        inTransaction(c -> {
            clearActivity(c, userId);
            update(c, "DELETE FROM holdings WHERE user_id = ?", userId);
            update(c, "DELETE FROM watchlists WHERE user_id = ?", userId);
            update(c, "DELETE FROM portfolio_snapshots WHERE user_id = ?", userId);
            try (PreparedStatement ps = c.prepareStatement("UPDATE wallets SET balance = ? WHERE user_id = ?")) {
                ps.setBigDecimal(1, UserRepository.STARTING_BALANCE);
                ps.setLong(2, userId);
                ps.executeUpdate();
            }
        });
    }

    // transactions reference orders without cascade, so they must go first
    private void clearActivity(Connection c, long userId) throws SQLException {
        update(c, "DELETE FROM transactions WHERE user_id = ?", userId);
        update(c, "DELETE FROM orders WHERE user_id = ?", userId);
    }

    private interface Body { void run(Connection c) throws SQLException; }

    private void inTransaction(Body body) throws DataAccessException {
        try (Connection c = Database.getConnection()) {
            c.setAutoCommit(false);
            try { body.run(c); c.commit(); }
            catch (SQLException e) { c.rollback(); throw e; }
        } catch (SQLException e) { throw SqlSupport.wrap("Admin operation failed", e); }
    }

    private void update(Connection c, String sql, long id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) { ps.setLong(1, id); ps.executeUpdate(); }
    }
}

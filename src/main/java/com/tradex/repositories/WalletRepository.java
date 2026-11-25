package com.tradex.repositories;

import com.tradex.database.Database;
import com.tradex.exceptions.DataAccessException;

import java.math.BigDecimal;
import java.sql.*;

public class WalletRepository {
    /** Reads the balance and locks the row until the transaction ends. */
    public BigDecimal lockBalance(Connection c, long userId) throws DataAccessException {
        try (PreparedStatement ps = c.prepareStatement("SELECT balance FROM wallets WHERE user_id = ? FOR UPDATE")) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new DataAccessException("Wallet not found", new SQLException("no wallet for " + userId));
                return rs.getBigDecimal(1);
            }
        } catch (SQLException e) { throw SqlSupport.wrap("Failed to read wallet", e); }
    }

    public void setBalance(Connection c, long userId, BigDecimal balance) throws DataAccessException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE wallets SET balance = ? WHERE user_id = ?")) {
            ps.setBigDecimal(1, balance);
            ps.setLong(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) { throw SqlSupport.wrap("Failed to update wallet", e); }
    }

    public BigDecimal getBalance(long userId) throws DataAccessException {
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement("SELECT balance FROM wallets WHERE user_id = ?")) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new DataAccessException("Wallet not found", new SQLException("no wallet for " + userId));
                return rs.getBigDecimal(1);
            }
        } catch (SQLException e) { throw SqlSupport.wrap("Failed to read wallet", e); }
    }
}

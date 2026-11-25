package com.tradex.repositories;

import com.tradex.database.Database;
import com.tradex.exceptions.DataAccessException;
import com.tradex.models.Role;
import com.tradex.models.User;

import java.math.BigDecimal;
import java.sql.*;
import java.util.Optional;

public class UserRepository {

    /** A user plus the stored hash, used only for credential checks. */
    public record Credentials(User user, String passwordHash) {}

    public static final BigDecimal STARTING_BALANCE = new BigDecimal("100000.00");

    /** Inserts the user and their starting wallet atomically. Returns false if username/email is taken. */
    public Optional<User> createUserWithWallet(String username, String email, String passwordHash) throws DataAccessException {
        try (Connection c = Database.getConnection()) {
            c.setAutoCommit(false);
            try (PreparedStatement ins = c.prepareStatement(
                    "INSERT INTO users (username, email, password_hash) VALUES (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ins.setString(1, username);
                ins.setString(2, email);
                ins.setString(3, passwordHash);
                ins.executeUpdate();
                long id;
                try (ResultSet keys = ins.getGeneratedKeys()) {
                    keys.next();
                    id = keys.getLong(1);
                }
                try (PreparedStatement w = c.prepareStatement("INSERT INTO wallets (user_id, balance) VALUES (?, ?)")) {
                    w.setLong(1, id);
                    w.setBigDecimal(2, STARTING_BALANCE);
                    w.executeUpdate();
                }
                c.commit();
                return Optional.of(new User(id, username, email, Role.USER));
            } catch (SQLIntegrityConstraintViolationException dup) {
                c.rollback();
                return Optional.empty();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to create user", e);
        }
    }

    public Optional<Credentials> findCredentials(String usernameOrEmail) throws DataAccessException {
        String sql = "SELECT id, username, email, role, password_hash FROM users WHERE username = ? OR email = ?";
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, usernameOrEmail);
            ps.setString(2, usernameOrEmail);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                User u = new User(rs.getLong("id"), rs.getString("username"), rs.getString("email"),
                        Role.valueOf(rs.getString("role")));
                return Optional.of(new Credentials(u, rs.getString("password_hash")));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to look up user", e);
        }
    }
}

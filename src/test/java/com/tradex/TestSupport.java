package com.tradex;

import com.tradex.database.Database;
import com.tradex.models.User;
import com.tradex.repositories.AdminRepository;
import com.tradex.repositories.UserRepository;
import com.tradex.services.AuthService;

import java.sql.*;
import java.util.UUID;

/** Helpers for integration tests that talk to the real MySQL database. */
public final class TestSupport {
    private TestSupport() {}

    public static boolean dbAvailable() {
        try (Connection c = Database.getConnection()) { return true; } catch (Exception e) { return false; }
    }

    public static User newUser(String prefix) throws Exception {
        String n = prefix + "_" + UUID.randomUUID().toString().substring(0, 8);
        return new AuthService(new UserRepository()).register(n, n + "@example.com", "password123");
    }

    /** Deletes a user and all their data (transactions and orders first, as the schema requires). */
    public static void deleteUser(User u) throws Exception { new AdminRepository().deleteUser(u.getId()); }

    public static void setPrice(String symbol, String price) throws Exception {
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement("UPDATE stocks SET current_price = ? WHERE symbol = ?")) {
            ps.setBigDecimal(1, new java.math.BigDecimal(price));
            ps.setString(2, symbol);
            ps.executeUpdate();
        }
    }

    public static void promoteToAdmin(User u) throws Exception {
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement("UPDATE users SET role = 'ADMIN' WHERE id = ?")) {
            ps.setLong(1, u.getId());
            ps.executeUpdate();
        }
    }
}

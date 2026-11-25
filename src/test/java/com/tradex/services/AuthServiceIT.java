package com.tradex.services;

import com.tradex.database.Database;
import com.tradex.exceptions.AuthenticationException;
import com.tradex.exceptions.ValidationException;
import com.tradex.models.User;
import com.tradex.repositories.UserRepository;
import org.junit.jupiter.api.*;

import java.sql.*;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Runs against the local MySQL database; skipped if it is unreachable. */
class AuthServiceIT {
    private final AuthService auth = new AuthService(new UserRepository());
    private final String name = "it_" + UUID.randomUUID().toString().substring(0, 8);

    @BeforeAll static void requireDb() {
        try (Connection c = Database.getConnection()) { /* ok */ }
        catch (Exception e) { Assumptions.abort("MySQL not available"); }
    }

    @AfterEach void cleanup() throws Exception {
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement("DELETE FROM users WHERE username = ?")) {
            ps.setString(1, name);
            ps.executeUpdate();
        }
    }

    @Test void registerCreatesWalletAndLoginWorks() throws Exception {
        User u = auth.register(name, name + "@example.com", "password123");
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement("SELECT balance FROM wallets WHERE user_id = ?")) {
            ps.setLong(1, u.getId());
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(0, rs.getBigDecimal(1).compareTo(UserRepository.STARTING_BALANCE));
            }
        }
        assertEquals(u.getId(), auth.login(name, "password123").getId());
        assertEquals(u.getId(), auth.login(name + "@EXAMPLE.com", "password123").getId());
        assertThrows(AuthenticationException.class, () -> auth.login(name, "wrongpass1"));
        assertThrows(AuthenticationException.class, () -> auth.login("nobody_here", "password123"));
    }

    @Test void duplicateRegistrationRejected() throws Exception {
        auth.register(name, name + "@example.com", "password123");
        assertThrows(ValidationException.class, () -> auth.register(name, "other_" + name + "@example.com", "password123"));
    }
}

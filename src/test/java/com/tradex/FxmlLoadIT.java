package com.tradex;

import com.tradex.database.Database;
import com.tradex.models.User;
import com.tradex.repositories.UserRepository;
import com.tradex.services.AuthService;
import com.tradex.utils.Session;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import org.junit.jupiter.api.*;

import java.sql.*;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/** Loads every FXML view on the FX thread to catch wiring errors (fx:id, handlers, bad markup). Needs MySQL + a display. */
class FxmlLoadIT {
    private static User user;
    private static boolean fxStarted;

    @BeforeAll static void setup() throws Exception {
        try (Connection c = Database.getConnection()) { /* ok */ }
        catch (Exception e) { Assumptions.abort("MySQL not available"); }
        String n = "fx_" + UUID.randomUUID().toString().substring(0, 8);
        user = new AuthService(new UserRepository()).register(n, n + "@example.com", "password123");
        Session.setCurrentUser(user);
        try { Platform.startup(() -> {}); fxStarted = true; }
        catch (IllegalStateException alreadyStarted) { fxStarted = true; }
        catch (Throwable t) { Assumptions.abort("JavaFX toolkit unavailable: " + t); }
    }

    @AfterAll static void cleanup() throws Exception {
        if (user == null) return;
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement("DELETE FROM users WHERE id = ?")) {
            ps.setLong(1, user.getId());
            ps.executeUpdate();
        }
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"login", "shell", "dashboard", "market", "portfolio", "watchlist", "transactions", "orders", "analytics", "leaderboard", "admin"})
    void viewLoads(String view) throws Exception {
        AtomicReference<Throwable> err = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);
        Platform.runLater(() -> {
            try { FXMLLoader.load(getClass().getResource("/fxml/" + view + ".fxml")); }
            catch (Throwable t) { err.set(t); }
            done.countDown();
        });
        assertTrue(done.await(20, TimeUnit.SECONDS));
        if (err.get() != null) fail("Failed to load " + view, err.get());
        Thread.sleep(500); // let async data tasks run so their callbacks are exercised too
    }
}

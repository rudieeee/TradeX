package com.tradex.services;

import com.tradex.database.Database;
import com.tradex.exceptions.*;
import com.tradex.models.*;
import com.tradex.repositories.UserRepository;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/** Runs against the local MySQL database; skipped if it is unreachable. */
class TradingServiceIT {
    private final TradingService trading = new TradingServiceImpl();
    private final String name = "tr_" + UUID.randomUUID().toString().substring(0, 8);
    private long uid;

    @BeforeAll static void requireDb() {
        try (Connection c = Database.getConnection()) { /* ok */ }
        catch (Exception e) { Assumptions.abort("MySQL not available"); }
    }

    @BeforeEach void user() throws Exception {
        uid = new AuthService(new UserRepository()).register(name, name + "@example.com", "password123").getId();
    }

    @AfterEach void cleanup() throws Exception {
        try (Connection c = Database.getConnection()) {
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM transactions WHERE user_id = ?")) { ps.setLong(1, uid); ps.executeUpdate(); }
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM orders WHERE user_id = ?")) { ps.setLong(1, uid); ps.executeUpdate(); }
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM users WHERE id = ?")) { ps.setLong(1, uid); ps.executeUpdate(); }
        }
    }

    private static BigDecimal bd(String s) { return new BigDecimal(s); }

    @Test void buyDebitsWalletAndCreatesHolding() throws Exception {
        trading.buy(uid, "tcs", 10); // 3900 each
        assertEquals(0, bd("61000.00").compareTo(trading.getBalance(uid)));
        Holding h = trading.getHoldings(uid).get(0);
        assertEquals(10, h.quantity());
        assertEquals(0, bd("3900").compareTo(h.avgPrice()));
        assertEquals(1, trading.getTransactions(uid).size());
    }

    @Test void sellCreditsWalletAndRemovesEmptyHolding() throws Exception {
        trading.buy(uid, "ITC", 100);   // 43000
        trading.sell(uid, "ITC", 40);
        assertEquals(60, trading.getHoldings(uid).get(0).quantity());
        trading.sell(uid, "ITC", 60);
        assertTrue(trading.getHoldings(uid).isEmpty());
        assertEquals(0, bd("100000").compareTo(trading.getBalance(uid)));
        assertEquals(3, trading.getTransactions(uid).size());
    }

    @Test void insufficientBalanceRollsBack() throws Exception {
        assertThrows(InsufficientBalanceException.class, () -> trading.buy(uid, "MARUTI", 9)); // 112500
        assertEquals(0, bd("100000").compareTo(trading.getBalance(uid)));
        assertTrue(trading.getTransactions(uid).isEmpty());
    }

    @Test void invalidOrdersRejected() {
        assertThrows(InvalidOrderException.class, () -> trading.buy(uid, "TCS", 0));
        assertThrows(InvalidOrderException.class, () -> trading.buy(uid, " ", 1));
        assertThrows(StockNotFoundException.class, () -> trading.buy(uid, "NOPE", 1));
        assertThrows(InsufficientHoldingsException.class, () -> trading.sell(uid, "TCS", 1));
    }

    @Test void concurrentBuysNeverOverspend() throws Exception {
        // 20 threads each try to buy 1 MARUTI (12500): only 8 fit in 100000.
        ExecutorService pool = Executors.newFixedThreadPool(20);
        List<Future<Boolean>> results = new ArrayList<>();
        for (int i = 0; i < 20; i++) results.add(pool.submit(() -> {
            try { trading.buy(uid, "MARUTI", 1); return true; } catch (InsufficientBalanceException e) { return false; }
        }));
        int ok = 0;
        for (Future<Boolean> f : results) if (f.get()) ok++;
        pool.shutdown();
        assertEquals(8, ok);
        assertEquals(0, BigDecimal.ZERO.compareTo(trading.getBalance(uid)));
        assertEquals(8, trading.getHoldings(uid).get(0).quantity());
    }

    @Test void averagePriceIsWeighted() throws Exception {
        trading.buy(uid, "INFY", 10);
        try (Connection c = Database.getConnection(); Statement s = c.createStatement()) {
            s.executeUpdate("UPDATE stocks SET current_price = 1850 WHERE symbol = 'INFY'");
        }
        try {
            trading.buy(uid, "INFY", 10);
            assertEquals(0, bd("1750.00").compareTo(trading.getHoldings(uid).get(0).avgPrice()));
        } finally {
            try (Connection c = Database.getConnection(); Statement s = c.createStatement()) {
                s.executeUpdate("UPDATE stocks SET current_price = 1650 WHERE symbol = 'INFY'");
            }
        }
    }
}

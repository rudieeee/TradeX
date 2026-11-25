package com.tradex.services;

import com.tradex.database.Database;
import com.tradex.exceptions.MarketDataException;
import com.tradex.models.Stock;
import com.tradex.repositories.PriceHistoryRepository;
import com.tradex.repositories.StockRepository;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/** Runs against the local MySQL database; restores stock prices afterwards. */
class PriceUpdaterIT {
    private final StockRepository stocks = new StockRepository();
    private final PriceHistoryRepository history = new PriceHistoryRepository();
    private Map<String, BigDecimal> snapshot;
    private int historyBefore;

    @BeforeAll static void requireDb() {
        try (Connection c = Database.getConnection()) { /* ok */ }
        catch (Exception e) { Assumptions.abort("MySQL not available"); }
    }

    @BeforeEach void snapshot() throws Exception {
        snapshot = new HashMap<>();
        for (Stock s : stocks.findAll()) snapshot.put(s.symbol(), s.currentPrice());
        historyBefore = history.count();
    }

    @AfterEach void restore() throws Exception {
        stocks.updatePrices(snapshot);
        if (historyBefore == 0) {
            try (Connection c = Database.getConnection()) { c.createStatement().executeUpdate("DELETE FROM price_history"); }
        } else {
            history.upsertToday(snapshot, LocalDateTime.now());
        }
    }

    private PriceUpdater updater(MarketDataProvider p) {
        return new PriceUpdater(p, stocks, history, new MarketService());
    }

    private BigDecimal price(String sym) throws Exception {
        return stocks.findAll().stream().filter(s -> s.symbol().equals(sym)).findFirst().orElseThrow().currentPrice();
    }

    @Test void providerFailureFallsBackToSimulation() throws Exception {
        var result = updater(syms -> { throw new MarketDataException("down"); }).refreshOnce();
        assertEquals(0, result.live());
        assertEquals(snapshot.size(), result.simulated());
        for (var e : snapshot.entrySet()) {
            double ratio = price(e.getKey()).doubleValue() / e.getValue().doubleValue();
            assertTrue(ratio > 0.98 && ratio < 1.02, e.getKey() + " moved too far: " + ratio);
        }
    }

    @Test void livePricesAreAppliedAndMissingOnesSimulated() throws Exception {
        BigDecimal live = snapshot.get("TCS").multiply(new BigDecimal("1.05")).setScale(2, java.math.RoundingMode.HALF_UP);
        var result = updater(syms -> Map.of("TCS", live)).refreshOnce();
        assertEquals(1, result.live());
        assertEquals(snapshot.size() - 1, result.simulated());
        assertEquals(0, live.compareTo(price("TCS")));
    }

    @Test void staleHistoryIsReseededOnBigPriceJump() throws Exception {
        new MarketService().ensureHistory();
        BigDecimal live = snapshot.get("INFY").multiply(new BigDecimal("0.5")).setScale(2, java.math.RoundingMode.HALF_UP);
        updater(syms -> Map.of("INFY", live)).refreshOnce();
        var pts = history.history("INFY", 5);
        BigDecimal last = pts.get(pts.size() - 1).price();
        assertTrue(last.doubleValue() < snapshot.get("INFY").doubleValue() * 0.7, "history should be near the new price");
    }
}

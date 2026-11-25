package com.tradex.services;

import com.tradex.models.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PerformanceCalculatorTest {
    private static long id = 0;
    private static Transaction tx(String sym, Side side, int qty, String price) {
        return new Transaction(++id, 1, 1, sym, side, qty, new BigDecimal(price), LocalDateTime.now());
    }

    @Test void realizedPnlUsesAverageCost() {
        var stats = PerformanceCalculator.analyse(List.of(
                tx("TCS", Side.BUY, 10, "100"), tx("TCS", Side.BUY, 10, "200"), // avg 150
                tx("TCS", Side.SELL, 5, "180")));                                 // +30 * 5
        assertEquals(0, new BigDecimal("150.00").compareTo(stats.realizedPnl()));
        assertEquals(3, stats.trades());
        assertEquals(1, stats.sells());
        assertEquals(100, stats.winRatePercent());
    }

    @Test void lossesAndWinRate() {
        var stats = PerformanceCalculator.analyse(List.of(
                tx("A", Side.BUY, 10, "100"), tx("A", Side.SELL, 10, "90"),   // -100
                tx("B", Side.BUY, 1, "50"), tx("B", Side.SELL, 1, "70")));    // +20
        assertEquals(0, new BigDecimal("-80.00").compareTo(stats.realizedPnl()));
        assertEquals(50, stats.winRatePercent());
    }

    @Test void resetsAverageAfterFullSale() {
        var stats = PerformanceCalculator.analyse(List.of(
                tx("A", Side.BUY, 1, "100"), tx("A", Side.SELL, 1, "100"),
                tx("A", Side.BUY, 1, "500"), tx("A", Side.SELL, 1, "510")));
        assertEquals(0, new BigDecimal("10.00").compareTo(stats.realizedPnl()));
    }

    @Test void handlesUnorderedInputAndEmpty() {
        var shuffled = List.of(tx("Z", Side.SELL, 1, "12"), tx("Z", Side.BUY, 1, "10"));
        // ids reflect creation order: the SELL was created first, so chronologically it precedes the BUY
        var stats = PerformanceCalculator.analyse(shuffled);
        assertEquals(2, stats.trades());
        assertEquals(0, PerformanceCalculator.analyse(List.of()).winRatePercent());
    }
}

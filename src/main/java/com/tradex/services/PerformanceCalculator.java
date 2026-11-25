package com.tradex.services;

import com.tradex.models.Side;
import com.tradex.models.Transaction;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/** Computes realised profit/loss from a trade history using average-cost accounting. */
public final class PerformanceCalculator {
    private PerformanceCalculator() {}

    public record Stats(BigDecimal realizedPnl, int trades, int sells, int winningSells) {
        /** Percentage of sells that realised a profit; 0 if there are no sells. */
        public int winRatePercent() { return sells == 0 ? 0 : (int) Math.round(100.0 * winningSells / sells); }
    }

    private static final class Position { int qty; BigDecimal avg = BigDecimal.ZERO; }

    public static Stats analyse(List<Transaction> transactions) {
        List<Transaction> chrono = new ArrayList<>(transactions);
        chrono.sort(Comparator.comparingLong(Transaction::id));
        Map<String, Position> positions = new HashMap<>();
        BigDecimal realized = BigDecimal.ZERO;
        int sells = 0, wins = 0;
        for (Transaction t : chrono) {
            Position p = positions.computeIfAbsent(t.symbol(), k -> new Position());
            BigDecimal q = BigDecimal.valueOf(t.quantity());
            if (t.side() == Side.BUY) {
                BigDecimal cost = p.avg.multiply(BigDecimal.valueOf(p.qty)).add(t.price().multiply(q));
                p.qty += t.quantity();
                p.avg = cost.divide(BigDecimal.valueOf(p.qty), 6, RoundingMode.HALF_UP);
            } else {
                BigDecimal gain = t.price().subtract(p.avg).multiply(q);
                realized = realized.add(gain);
                sells++;
                if (gain.signum() > 0) wins++;
                p.qty -= t.quantity();
                if (p.qty <= 0) { p.qty = 0; p.avg = BigDecimal.ZERO; }
            }
        }
        return new Stats(realized.setScale(2, RoundingMode.HALF_UP), chrono.size(), sells, wins);
    }
}

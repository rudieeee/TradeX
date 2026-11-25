package com.tradex.services;

import com.tradex.exceptions.DataAccessException;
import com.tradex.models.PricePoint;
import com.tradex.repositories.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class AnalyticsService {
    public record Analytics(PortfolioService.Summary summary, PerformanceCalculator.Stats stats,
                            List<PricePoint> valueHistory, BigDecimal totalReturnPct) {}

    private static final int HISTORY_POINTS = 300;

    private final PortfolioService portfolio = new PortfolioService();
    private final TransactionRepository transactions = new TransactionRepository();
    private final SnapshotRepository snapshots = new SnapshotRepository();

    public Analytics getAnalytics(long userId) throws DataAccessException {
        var summary = portfolio.getSummary(userId);
        var stats = PerformanceCalculator.analyse(transactions.findByUser(userId));
        BigDecimal start = UserRepository.STARTING_BALANCE;
        BigDecimal ret = summary.totalValue().subtract(start).multiply(BigDecimal.valueOf(100)).divide(start, 2, RoundingMode.HALF_UP);
        return new Analytics(summary, stats, snapshots.history(userId, HISTORY_POINTS), ret);
    }
}

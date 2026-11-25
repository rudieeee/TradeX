package com.tradex.services;

import com.tradex.exceptions.TradeXException;
import com.tradex.models.Stock;
import com.tradex.repositories.PriceHistoryRepository;
import com.tradex.repositories.StockRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/** Background task that refreshes stock prices from a provider, falling back to a simulation per symbol. */
public class PriceUpdater {
    public record Result(int live, int simulated) {
        public boolean isLive() { return live > 0; }
    }

    /** If a live price differs from the stored one by more than this ratio, the stored history is stale. */
    private static final double RESEED_RATIO = 1.3;

    private final MarketDataProvider provider;
    private final StockRepository stocks;
    private final PriceHistoryRepository history;
    private final MarketService market;
    private final Random rnd = new Random();
    private ScheduledExecutorService scheduler;

    public PriceUpdater(MarketDataProvider provider, StockRepository stocks, PriceHistoryRepository history, MarketService market) {
        this.provider = provider;
        this.stocks = stocks;
        this.history = history;
        this.market = market;
    }

    public synchronized void start(long periodSeconds, Consumer<Result> onUpdate) {
        if (scheduler != null) return;
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "price-updater");
            t.setDaemon(true);
            return t;
        });
        // scheduleWithFixedDelay: an exception must not cancel future runs, so everything is caught.
        scheduler.scheduleWithFixedDelay(() -> {
            try { onUpdate.accept(refreshOnce()); }
            catch (Exception e) { System.err.println("Price update failed: " + e.getMessage()); }
        }, 0, periodSeconds, TimeUnit.SECONDS);
    }

    public synchronized void stop() {
        if (scheduler != null) { scheduler.shutdownNow(); scheduler = null; }
    }

    /** One refresh cycle: fetch, fall back to simulation for missing symbols, persist. */
    public Result refreshOnce() throws TradeXException {
        List<Stock> current = stocks.findAll();
        Map<String, BigDecimal> live;
        try {
            live = provider.fetchPrices(current.stream().map(Stock::symbol).toList());
        } catch (TradeXException e) {
            System.err.println("Live prices unavailable, simulating: " + e.getMessage());
            live = Map.of();
        }
        Map<String, BigDecimal> updated = new LinkedHashMap<>();
        int liveCount = 0;
        for (Stock s : current) {
            BigDecimal p = live.get(s.symbol());
            if (p != null) {
                liveCount++;
                double ratio = p.doubleValue() / s.currentPrice().doubleValue();
                if (ratio > RESEED_RATIO || ratio < 1 / RESEED_RATIO) market.reseed(s.symbol(), p);
            } else {
                p = SimulatedPriceModel.next(s.currentPrice(), rnd);
            }
            updated.put(s.symbol(), p);
        }
        stocks.updatePrices(updated);
        market.ensureHistory();
        history.upsertToday(updated, LocalDateTime.now());
        return new Result(liveCount, current.size() - liveCount);
    }
}

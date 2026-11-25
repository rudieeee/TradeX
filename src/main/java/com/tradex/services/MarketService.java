package com.tradex.services;

import com.tradex.exceptions.DataAccessException;
import com.tradex.models.*;
import com.tradex.repositories.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

public class MarketService {
    static final int HISTORY_DAYS = 60;

    private final StockRepository stocks = new StockRepository();
    private final PriceHistoryRepository history = new PriceHistoryRepository();

    public List<Quote> getQuotes() throws DataAccessException {
        Map<String, BigDecimal> prev = history.priceBefore(LocalDate.now().atStartOfDay());
        List<Quote> out = new ArrayList<>();
        for (Stock s : stocks.findAll()) out.add(new Quote(s, prev.getOrDefault(s.symbol(), s.currentPrice())));
        return out;
    }

    /** Daily closes for the chart, ending with the live price. */
    public List<PricePoint> getHistory(String symbol, Stock current) throws DataAccessException {
        List<PricePoint> pts = new ArrayList<>(history.history(symbol, HISTORY_DAYS));
        pts.add(new PricePoint(LocalDateTime.now(), current.currentPrice()));
        return pts;
    }

    /** Generates a deterministic 60-day random walk ending near each stock's current price, if none exists. */
    public void ensureHistory() throws DataAccessException {
        if (history.count() > 0) return;
        List<Object[]> rows = new ArrayList<>();
        for (Stock s : stocks.findAll()) {
            Random rnd = new Random(s.symbol().hashCode());
            double p = s.currentPrice().doubleValue() * (1 + rnd.nextGaussian() * 0.01); // yesterday's close
            for (int d = 1; d <= HISTORY_DAYS; d++) {
                rows.add(new Object[]{s.symbol(), BigDecimal.valueOf(p).setScale(2, RoundingMode.HALF_UP),
                        LocalDate.now().minusDays(d).atTime(15, 30)});
                p = p / (1 + rnd.nextGaussian() * 0.012);
            }
        }
        history.insertBatch(rows);
    }
}

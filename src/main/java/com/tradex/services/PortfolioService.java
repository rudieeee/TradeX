package com.tradex.services;

import com.tradex.exceptions.DataAccessException;
import com.tradex.models.*;
import com.tradex.repositories.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public class PortfolioService {
    private final HoldingRepository holdings = new HoldingRepository();
    private final StockRepository stocks = new StockRepository();
    private final WalletRepository wallets = new WalletRepository();

    public record Summary(List<PortfolioRow> rows, BigDecimal cash) {
        public BigDecimal invested() { return rows.stream().map(PortfolioRow::invested).reduce(BigDecimal.ZERO, BigDecimal::add); }
        public BigDecimal marketValue() { return rows.stream().map(PortfolioRow::value).reduce(BigDecimal.ZERO, BigDecimal::add); }
        public BigDecimal pnl() { return marketValue().subtract(invested()); }
        public BigDecimal pnlPct() {
            return invested().signum() == 0 ? BigDecimal.ZERO
                    : pnl().multiply(BigDecimal.valueOf(100)).divide(invested(), 2, RoundingMode.HALF_UP);
        }
        public BigDecimal totalValue() { return cash.add(marketValue()); }
    }

    public Summary getSummary(long userId) throws DataAccessException {
        Map<String, Stock> bysymbol = new HashMap<>();
        for (Stock s : stocks.findAll()) bysymbol.put(s.symbol(), s);
        List<PortfolioRow> rows = new ArrayList<>();
        for (Holding h : holdings.findByUser(userId)) {
            Stock s = bysymbol.get(h.symbol());
            rows.add(new PortfolioRow(h.symbol(), s.name(), h.quantity(), h.avgPrice(), s.currentPrice()));
        }
        return new Summary(rows, wallets.getBalance(userId));
    }
}

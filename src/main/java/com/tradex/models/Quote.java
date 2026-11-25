package com.tradex.models;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** A stock with its previous close, for day-change display. */
public record Quote(Stock stock, BigDecimal prevClose) {
    public String symbol() { return stock.symbol(); }
    public BigDecimal price() { return stock.currentPrice(); }
    public BigDecimal change() { return price().subtract(prevClose); }
    public BigDecimal changePct() {
        if (prevClose.signum() == 0) return BigDecimal.ZERO;
        return change().multiply(BigDecimal.valueOf(100)).divide(prevClose, 2, RoundingMode.HALF_UP);
    }
}

package com.tradex.models;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record PortfolioRow(String symbol, String name, int quantity, BigDecimal avgPrice, BigDecimal ltp) {
    public BigDecimal invested() { return avgPrice.multiply(BigDecimal.valueOf(quantity)); }
    public BigDecimal value() { return ltp.multiply(BigDecimal.valueOf(quantity)); }
    public BigDecimal pnl() { return value().subtract(invested()); }
    public BigDecimal pnlPct() {
        if (invested().signum() == 0) return BigDecimal.ZERO;
        return pnl().multiply(BigDecimal.valueOf(100)).divide(invested(), 2, RoundingMode.HALF_UP);
    }
}

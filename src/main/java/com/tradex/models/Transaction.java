package com.tradex.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Transaction(long id, long orderId, long userId, String symbol, Side side,
                          int quantity, BigDecimal price, LocalDateTime executedAt) {
    public BigDecimal total() { return price.multiply(BigDecimal.valueOf(quantity)); }
}

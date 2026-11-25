package com.tradex.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** A stored order. {@code triggerPrice} is null for market orders. */
public record OrderRecord(long id, long userId, String symbol, Side side, OrderType type, int quantity,
                          BigDecimal triggerPrice, OrderStatus status, LocalDateTime createdAt) {
    public Order toOrder() {
        return side == Side.BUY ? new BuyOrder(userId, symbol, quantity) : new SellOrder(userId, symbol, quantity);
    }
}

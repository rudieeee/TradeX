package com.tradex.services;

import com.tradex.models.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class OrderTriggerTest {
    private static OrderRecord order(Side side, OrderType type, String trigger) {
        return new OrderRecord(1, 1, "TCS", side, type, 1, trigger == null ? null : new BigDecimal(trigger),
                OrderStatus.PENDING, LocalDateTime.now());
    }
    private static boolean fires(Side s, OrderType t, String trigger, String market) {
        return OrderTrigger.shouldFire(order(s, t, trigger), new BigDecimal(market));
    }

    @Test void limitBuyFiresAtOrBelowLimit() {
        assertTrue(fires(Side.BUY, OrderType.LIMIT, "100", "99.99"));
        assertTrue(fires(Side.BUY, OrderType.LIMIT, "100", "100"));
        assertFalse(fires(Side.BUY, OrderType.LIMIT, "100", "100.01"));
    }

    @Test void limitSellFiresAtOrAboveLimit() {
        assertTrue(fires(Side.SELL, OrderType.LIMIT, "100", "100"));
        assertTrue(fires(Side.SELL, OrderType.LIMIT, "100", "120"));
        assertFalse(fires(Side.SELL, OrderType.LIMIT, "100", "99.99"));
    }

    @Test void stopLossFiresAtOrBelowTriggerAndOnlyForSells() {
        assertTrue(fires(Side.SELL, OrderType.STOP_LOSS, "100", "100"));
        assertTrue(fires(Side.SELL, OrderType.STOP_LOSS, "100", "80"));
        assertFalse(fires(Side.SELL, OrderType.STOP_LOSS, "100", "100.01"));
        assertFalse(fires(Side.BUY, OrderType.STOP_LOSS, "100", "50"));
    }

    @Test void orderWithoutTriggerNeverFires() {
        assertFalse(OrderTrigger.shouldFire(order(Side.BUY, OrderType.LIMIT, null), BigDecimal.ONE));
    }
}

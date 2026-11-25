package com.tradex.services;

import com.tradex.models.*;

import java.math.BigDecimal;

/** Decides whether a pending order should execute at the given market price. */
public final class OrderTrigger {
    private OrderTrigger() {}

    public static boolean shouldFire(OrderRecord o, BigDecimal marketPrice) {
        BigDecimal t = o.triggerPrice();
        if (t == null) return false;
        return switch (o.type()) {
            case MARKET -> true;
            case LIMIT -> o.side() == Side.BUY ? marketPrice.compareTo(t) <= 0 : marketPrice.compareTo(t) >= 0;
            case STOP_LOSS -> o.side() == Side.SELL && marketPrice.compareTo(t) <= 0;
        };
    }
}

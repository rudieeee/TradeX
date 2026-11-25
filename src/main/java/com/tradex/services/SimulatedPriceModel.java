package com.tradex.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Random;

/** Random-walk price movement used when live data is unavailable. */
public final class SimulatedPriceModel {
    private static final double SIGMA = 0.0025; // ~0.25% per tick
    private static final double MAX_MOVE = 0.01;
    private static final BigDecimal MIN_PRICE = new BigDecimal("1.00");

    private SimulatedPriceModel() {}

    public static BigDecimal next(BigDecimal current, Random rnd) {
        double move = Math.max(-MAX_MOVE, Math.min(MAX_MOVE, rnd.nextGaussian() * SIGMA));
        BigDecimal p = current.multiply(BigDecimal.valueOf(1 + move)).setScale(2, RoundingMode.HALF_UP);
        return p.max(MIN_PRICE);
    }
}

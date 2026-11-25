package com.tradex.models;

import java.math.BigDecimal;

public record Holding(long userId, String symbol, int quantity, BigDecimal avgPrice) {}

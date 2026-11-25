package com.tradex.models;

import java.math.BigDecimal;

public record Stock(String symbol, String name, BigDecimal currentPrice) {}

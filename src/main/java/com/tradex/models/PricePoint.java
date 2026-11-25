package com.tradex.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PricePoint(LocalDateTime time, BigDecimal price) {}

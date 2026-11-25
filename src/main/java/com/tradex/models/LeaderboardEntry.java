package com.tradex.models;

import java.math.BigDecimal;

public record LeaderboardEntry(int rank, long userId, String username, BigDecimal totalValue, BigDecimal returnPct) {}

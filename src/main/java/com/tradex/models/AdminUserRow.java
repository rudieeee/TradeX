package com.tradex.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminUserRow(long id, String username, String email, Role role, LocalDateTime createdAt,
                           BigDecimal balance, int trades) {}

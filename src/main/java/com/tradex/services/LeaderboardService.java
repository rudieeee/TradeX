package com.tradex.services;

import com.tradex.exceptions.DataAccessException;
import com.tradex.models.LeaderboardEntry;
import com.tradex.repositories.LeaderboardRepository;
import com.tradex.repositories.UserRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public class LeaderboardService {
    private final LeaderboardRepository repo = new LeaderboardRepository();

    public List<LeaderboardEntry> getLeaderboard() throws DataAccessException {
        BigDecimal start = UserRepository.STARTING_BALANCE;
        List<LeaderboardEntry> out = new ArrayList<>();
        int rank = 1;
        for (var s : repo.standings()) {
            BigDecimal ret = s.totalValue().subtract(start).multiply(BigDecimal.valueOf(100)).divide(start, 2, RoundingMode.HALF_UP);
            out.add(new LeaderboardEntry(rank++, s.userId(), s.username(), s.totalValue(), ret));
        }
        return out;
    }
}

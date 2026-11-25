package com.tradex.services;

import com.tradex.exceptions.DataAccessException;
import com.tradex.repositories.WatchlistRepository;

import java.util.Set;

public class WatchlistService {
    private final WatchlistRepository repo = new WatchlistRepository();

    public Set<String> symbols(long userId) throws DataAccessException { return repo.symbols(userId); }
    public void add(long userId, String symbol) throws DataAccessException { repo.add(userId, symbol); }
    public void remove(long userId, String symbol) throws DataAccessException { repo.remove(userId, symbol); }
}

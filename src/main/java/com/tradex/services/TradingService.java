package com.tradex.services;

import com.tradex.exceptions.TradeXException;
import com.tradex.models.*;

import java.math.BigDecimal;
import java.util.List;

/** Trading operations exposed to controllers. */
public interface TradingService {
    Transaction buy(long userId, String symbol, int quantity) throws TradeXException;
    Transaction sell(long userId, String symbol, int quantity) throws TradeXException;
    BigDecimal getBalance(long userId) throws TradeXException;
    List<Holding> getHoldings(long userId) throws TradeXException;
    List<Transaction> getTransactions(long userId) throws TradeXException;
}

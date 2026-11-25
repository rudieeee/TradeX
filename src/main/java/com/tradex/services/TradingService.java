package com.tradex.services;

import com.tradex.exceptions.TradeXException;
import com.tradex.models.*;

import java.math.BigDecimal;
import java.util.List;

/** Trading operations exposed to controllers. */
public interface TradingService {
    Transaction buy(long userId, String symbol, int quantity) throws TradeXException;
    Transaction sell(long userId, String symbol, int quantity) throws TradeXException;

    /** LIMIT: buy at or below / sell at or above {@code price}. STOP_LOSS: sell when the market falls to {@code price}. */
    OrderRecord placeConditionalOrder(long userId, String symbol, Side side, OrderType type, int quantity, BigDecimal price)
            throws TradeXException;
    void cancelOrder(long userId, long orderId) throws TradeXException;
    List<OrderRecord> getOrders(long userId) throws TradeXException;
    /** Executes every pending order whose trigger condition is met. Returns the number executed. */
    int processPendingOrders() throws TradeXException;

    BigDecimal getBalance(long userId) throws TradeXException;
    List<Holding> getHoldings(long userId) throws TradeXException;
    List<Transaction> getTransactions(long userId) throws TradeXException;
}

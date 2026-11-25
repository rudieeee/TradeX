package com.tradex.services;

import com.tradex.database.Database;
import com.tradex.exceptions.*;
import com.tradex.models.*;
import com.tradex.repositories.*;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class TradingServiceImpl implements TradingService {
    private static final int MAX_QUANTITY = 100_000;

    private final StockRepository stocks = new StockRepository();
    private final WalletRepository wallets = new WalletRepository();
    private final HoldingRepository holdings = new HoldingRepository();
    private final TransactionRepository transactions = new TransactionRepository();
    /** One monitor per user so concurrent trades by the same user in this JVM never interleave. */
    private final ConcurrentHashMap<Long, Object> userLocks = new ConcurrentHashMap<>();

    @Override public Transaction buy(long userId, String symbol, int quantity) throws TradeXException {
        return place(new BuyOrder(userId, normalize(symbol), quantity));
    }

    @Override public Transaction sell(long userId, String symbol, int quantity) throws TradeXException {
        return place(new SellOrder(userId, normalize(symbol), quantity));
    }

    private Transaction place(Order order) throws TradeXException {
        if (order.getQuantity() <= 0 || order.getQuantity() > MAX_QUANTITY) {
            throw new InvalidOrderException("Quantity must be between 1 and " + MAX_QUANTITY);
        }
        // The JVM lock serialises same-user trades; SELECT ... FOR UPDATE protects against other processes.
        synchronized (userLocks.computeIfAbsent(order.getUserId(), k -> new Object())) {
            try (Connection c = Database.getConnection()) {
                c.setAutoCommit(false);
                try {
                    Stock stock = stocks.find(c, order.getSymbol())
                            .orElseThrow(() -> new StockNotFoundException("Unknown stock: " + order.getSymbol()));
                    BigDecimal price = stock.currentPrice();
                    order.execute(c, price, wallets, holdings);
                    long orderId = transactions.insertExecutedOrder(c, order);
                    transactions.insertTransaction(c, orderId, order, price);
                    c.commit();
                    return new Transaction(0, orderId, order.getUserId(), order.getSymbol(), order.getSide(),
                            order.getQuantity(), price, java.time.LocalDateTime.now());
                } catch (TradeXException | RuntimeException e) {
                    c.rollback();
                    throw e;
                }
            } catch (SQLException e) {
                throw new DataAccessException("Trade failed", e);
            }
        }
    }

    private static String normalize(String symbol) throws InvalidOrderException {
        if (symbol == null || symbol.isBlank()) throw new InvalidOrderException("Select a stock");
        return symbol.trim().toUpperCase();
    }

    @Override public BigDecimal getBalance(long userId) throws TradeXException { return wallets.getBalance(userId); }
    @Override public List<Holding> getHoldings(long userId) throws TradeXException { return holdings.findByUser(userId); }
    @Override public List<Transaction> getTransactions(long userId) throws TradeXException { return transactions.findByUser(userId); }
}

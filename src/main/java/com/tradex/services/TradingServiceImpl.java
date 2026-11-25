package com.tradex.services;

import com.tradex.database.Database;
import com.tradex.exceptions.*;
import com.tradex.models.*;
import com.tradex.repositories.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TradingServiceImpl implements TradingService {
    private static final int MAX_QUANTITY = 100_000;

    private final StockRepository stocks = new StockRepository();
    private final WalletRepository wallets = new WalletRepository();
    private final HoldingRepository holdings = new HoldingRepository();
    private final TransactionRepository transactions = new TransactionRepository();
    private final OrderRepository orders = new OrderRepository();
    /** One monitor per user so concurrent trades by the same user in this JVM never interleave. */
    private static final ConcurrentHashMap<Long, Object> USER_LOCKS = new ConcurrentHashMap<>();

    @Override public Transaction buy(long userId, String symbol, int quantity) throws TradeXException {
        return placeMarket(new BuyOrder(userId, normalize(symbol), quantity));
    }

    @Override public Transaction sell(long userId, String symbol, int quantity) throws TradeXException {
        return placeMarket(new SellOrder(userId, normalize(symbol), quantity));
    }

    private Transaction placeMarket(Order order) throws TradeXException {
        validateQuantity(order.getQuantity());
        // The JVM lock serialises same-user trades; SELECT ... FOR UPDATE protects against other processes.
        synchronized (lockFor(order.getUserId())) {
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
                            order.getQuantity(), price, LocalDateTime.now());
                } catch (TradeXException | RuntimeException e) {
                    c.rollback();
                    throw e;
                }
            } catch (SQLException e) {
                throw new DataAccessException("Trade failed", e);
            }
        }
    }

    @Override
    public OrderRecord placeConditionalOrder(long userId, String symbol, Side side, OrderType type, int quantity, BigDecimal price)
            throws TradeXException {
        String sym = normalize(symbol);
        validateQuantity(quantity);
        if (type == OrderType.MARKET) throw new InvalidOrderException("Use buy/sell for market orders");
        if (type == OrderType.STOP_LOSS && side != Side.SELL) throw new InvalidOrderException("Stop-loss orders can only be sell orders");
        if (price == null || price.signum() <= 0) throw new InvalidOrderException("Enter a trigger price above zero");
        BigDecimal trigger = price.setScale(2, RoundingMode.HALF_UP);
        if (trigger.signum() <= 0) throw new InvalidOrderException("Enter a trigger price above zero");

        var stock = stocks.findAll().stream().filter(s -> s.symbol().equals(sym)).findFirst()
                .orElseThrow(() -> new StockNotFoundException("Unknown stock: " + sym));
        if (side == Side.SELL) {
            int owned = holdings.findByUser(userId).stream().filter(h -> h.symbol().equals(sym)).mapToInt(Holding::quantity).sum();
            if (owned < quantity) throw new InsufficientHoldingsException("You own " + owned + " of " + sym + ", cannot place a sell for " + quantity);
        }
        long id = orders.insertPending(userId, stock.symbol(), side, type, quantity, trigger);
        processPendingOrders(); // fill immediately if the condition already holds
        return orders.find(id).orElseThrow();
    }

    @Override
    public void cancelOrder(long userId, long orderId) throws TradeXException {
        if (!orders.cancel(userId, orderId)) throw new InvalidOrderException("Only your own pending orders can be cancelled");
    }

    @Override public List<OrderRecord> getOrders(long userId) throws TradeXException { return orders.findByUser(userId); }

    @Override
    public int processPendingOrders() throws TradeXException {
        Map<String, BigDecimal> prices = new HashMap<>();
        for (Stock s : stocks.findAll()) prices.put(s.symbol(), s.currentPrice());
        int executed = 0;
        for (OrderRecord o : orders.findAllPending()) {
            BigDecimal px = prices.get(o.symbol());
            if (px != null && OrderTrigger.shouldFire(o, px) && fill(o)) executed++;
        }
        return executed;
    }

    /** Executes one pending order at the current price. Returns true if it was filled. */
    private boolean fill(OrderRecord rec) throws TradeXException {
        synchronized (lockFor(rec.userId())) {
            try (Connection c = Database.getConnection()) {
                c.setAutoCommit(false);
                try {
                    if (!orders.lockIfPending(c, rec.id())) { c.rollback(); return false; } // cancelled/filled meanwhile
                    BigDecimal price = stocks.find(c, rec.symbol()).orElseThrow(() -> new StockNotFoundException(rec.symbol())).currentPrice();
                    if (!OrderTrigger.shouldFire(rec, price)) { c.rollback(); return false; }
                    Order order = rec.toOrder();
                    try {
                        order.execute(c, price, wallets, holdings);
                    } catch (InsufficientBalanceException | InsufficientHoldingsException e) {
                        // Pending orders reserve nothing, so the funds/shares may be gone by trigger time.
                        orders.markStatus(c, rec.id(), OrderStatus.REJECTED);
                        c.commit();
                        return false;
                    }
                    orders.markStatus(c, rec.id(), OrderStatus.EXECUTED);
                    transactions.insertTransaction(c, rec.id(), order, price);
                    c.commit();
                    return true;
                } catch (TradeXException | RuntimeException e) {
                    c.rollback();
                    throw e;
                }
            } catch (SQLException e) {
                throw new DataAccessException("Order execution failed", e);
            }
        }
    }

    private static Object lockFor(long userId) { return USER_LOCKS.computeIfAbsent(userId, k -> new Object()); }

    private static void validateQuantity(int q) throws InvalidOrderException {
        if (q <= 0 || q > MAX_QUANTITY) throw new InvalidOrderException("Quantity must be between 1 and " + MAX_QUANTITY);
    }

    private static String normalize(String symbol) throws InvalidOrderException {
        if (symbol == null || symbol.isBlank()) throw new InvalidOrderException("Select a stock");
        return symbol.trim().toUpperCase();
    }

    @Override public BigDecimal getBalance(long userId) throws TradeXException { return wallets.getBalance(userId); }
    @Override public List<Holding> getHoldings(long userId) throws TradeXException { return holdings.findByUser(userId); }
    @Override public List<Transaction> getTransactions(long userId) throws TradeXException { return transactions.findByUser(userId); }
}

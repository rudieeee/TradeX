package com.tradex.services;

import com.tradex.TestSupport;
import com.tradex.exceptions.*;
import com.tradex.models.*;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class ConditionalOrdersIT {
    private final TradingService trading = new TradingServiceImpl();
    private User user;

    @BeforeAll static void requireDb() { Assumptions.assumeTrue(TestSupport.dbAvailable(), "MySQL not available"); }

    @BeforeEach void setup() throws Exception {
        TestSupport.setPrice("ITC", "400.00");
        TestSupport.setPrice("WIPRO", "500.00");
        user = TestSupport.newUser("co");
    }

    @AfterEach void cleanup() throws Exception { TestSupport.deleteUser(user); }

    private OrderRecord only() throws Exception { return trading.getOrders(user.getId()).get(0); }
    private static BigDecimal bd(String s) { return new BigDecimal(s); }

    @Test void limitBuyWaitsThenFillsWhenPriceDrops() throws Exception {
        OrderRecord o = trading.placeConditionalOrder(user.getId(), "ITC", Side.BUY, OrderType.LIMIT, 10, bd("380"));
        assertEquals(OrderStatus.PENDING, o.status());
        assertEquals(0, trading.processPendingOrders());
        assertEquals(bd("100000").compareTo(trading.getBalance(user.getId())), 0);

        TestSupport.setPrice("ITC", "375.00");
        assertEquals(1, trading.processPendingOrders());
        assertEquals(OrderStatus.EXECUTED, only().status());
        assertEquals(0, bd("96250").compareTo(trading.getBalance(user.getId()))); // filled at market 375, not limit 380
        assertEquals(10, trading.getHoldings(user.getId()).get(0).quantity());
        assertEquals(1, trading.getTransactions(user.getId()).size());
    }

    @Test void stopLossSellsWhenPriceFalls() throws Exception {
        trading.buy(user.getId(), "ITC", 50);   // 20000
        trading.placeConditionalOrder(user.getId(), "ITC", Side.SELL, OrderType.STOP_LOSS, 50, bd("380"));
        assertEquals(0, trading.processPendingOrders());
        TestSupport.setPrice("ITC", "379.00");
        assertEquals(1, trading.processPendingOrders());
        assertTrue(trading.getHoldings(user.getId()).isEmpty());
        assertEquals(0, bd("98950").compareTo(trading.getBalance(user.getId())));
    }

    @Test void limitSellFillsImmediatelyWhenAlreadyInTheMoney() throws Exception {
        trading.buy(user.getId(), "ITC", 10);
        OrderRecord o = trading.placeConditionalOrder(user.getId(), "ITC", Side.SELL, OrderType.LIMIT, 10, bd("390"));
        assertEquals(OrderStatus.EXECUTED, o.status()); // market 400 >= 390
    }

    @Test void cancelledOrderNeverFires() throws Exception {
        OrderRecord o = trading.placeConditionalOrder(user.getId(), "ITC", Side.BUY, OrderType.LIMIT, 1, bd("300"));
        trading.cancelOrder(user.getId(), o.id());
        TestSupport.setPrice("ITC", "250.00");
        assertEquals(0, trading.processPendingOrders());
        assertEquals(OrderStatus.CANCELLED, only().status());
        assertThrows(InvalidOrderException.class, () -> trading.cancelOrder(user.getId(), o.id())); // already cancelled
    }

    @Test void cannotCancelAnotherUsersOrder() throws Exception {
        OrderRecord o = trading.placeConditionalOrder(user.getId(), "ITC", Side.BUY, OrderType.LIMIT, 1, bd("300"));
        User other = TestSupport.newUser("co2");
        try {
            assertThrows(InvalidOrderException.class, () -> trading.cancelOrder(other.getId(), o.id()));
            assertEquals(OrderStatus.PENDING, only().status());
        } finally { TestSupport.deleteUser(other); }
    }

    @Test void orderRejectedIfFundsGoneByTriggerTime() throws Exception {
        trading.placeConditionalOrder(user.getId(), "WIPRO", Side.BUY, OrderType.LIMIT, 150, bd("450")); // needs ~67k
        trading.buy(user.getId(), "ITC", 200);                                                           // spends 80k
        TestSupport.setPrice("WIPRO", "440.00");
        assertEquals(0, trading.processPendingOrders());
        assertEquals(OrderStatus.REJECTED, trading.getOrders(user.getId()).stream()
                .filter(o -> o.symbol().equals("WIPRO")).findFirst().orElseThrow().status());
        assertEquals(1, trading.getTransactions(user.getId()).size());
    }

    @Test void validation() throws Exception {
        long u = user.getId();
        assertThrows(InvalidOrderException.class, () -> trading.placeConditionalOrder(u, "ITC", Side.BUY, OrderType.STOP_LOSS, 1, bd("300")));
        assertThrows(InvalidOrderException.class, () -> trading.placeConditionalOrder(u, "ITC", Side.BUY, OrderType.MARKET, 1, bd("300")));
        assertThrows(InvalidOrderException.class, () -> trading.placeConditionalOrder(u, "ITC", Side.BUY, OrderType.LIMIT, 1, bd("0")));
        assertThrows(InvalidOrderException.class, () -> trading.placeConditionalOrder(u, "ITC", Side.BUY, OrderType.LIMIT, 0, bd("300")));
        assertThrows(StockNotFoundException.class, () -> trading.placeConditionalOrder(u, "NOPE", Side.BUY, OrderType.LIMIT, 1, bd("300")));
        assertThrows(InsufficientHoldingsException.class, () -> trading.placeConditionalOrder(u, "ITC", Side.SELL, OrderType.LIMIT, 1, bd("300")));
    }
}

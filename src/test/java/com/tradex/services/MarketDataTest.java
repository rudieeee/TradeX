package com.tradex.services;

import com.tradex.exceptions.MarketDataException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class MarketDataTest {
    @Test void parsesYahooPrice() throws Exception {
        String json = "{\"chart\":{\"result\":[{\"meta\":{\"symbol\":\"TCS.NS\",\"regularMarketPrice\":2515.4}}],\"error\":null}}";
        assertEquals(0, new BigDecimal("2515.40").compareTo(YahooFinanceProvider.parsePrice(json)));
    }

    @Test void rejectsBadYahooResponses() {
        assertThrows(MarketDataException.class, () -> YahooFinanceProvider.parsePrice("not json"));
        assertThrows(MarketDataException.class, () -> YahooFinanceProvider.parsePrice("{\"chart\":{\"result\":null}}"));
        assertThrows(MarketDataException.class, () -> YahooFinanceProvider.parsePrice(
                "{\"chart\":{\"result\":[{\"meta\":{\"regularMarketPrice\":0}}]}}"));
    }

    @Test void simulatedMovesStayBoundedAndPositive() {
        Random rnd = new Random(1);
        BigDecimal p = new BigDecimal("100.00");
        for (int i = 0; i < 10_000; i++) {
            BigDecimal n = SimulatedPriceModel.next(p, rnd);
            assertTrue(n.subtract(p).abs().doubleValue() <= p.doubleValue() * 0.0101, "move too large");
            assertTrue(n.signum() > 0);
            p = n;
        }
        assertTrue(SimulatedPriceModel.next(new BigDecimal("1.00"), new Random(2)).compareTo(BigDecimal.ONE) >= 0);
    }
}

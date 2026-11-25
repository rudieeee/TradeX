package com.tradex.services;

import com.tradex.exceptions.MarketDataException;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;

/** Source of live prices. May return prices for only some of the requested symbols. */
public interface MarketDataProvider {
    Map<String, BigDecimal> fetchPrices(Collection<String> symbols) throws MarketDataException;
}

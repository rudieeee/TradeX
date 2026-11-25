package com.tradex.exceptions;

/** The external market data source could not be reached or returned unusable data. */
public class MarketDataException extends TradeXException {
    public MarketDataException(String message) { super(message); }
    public MarketDataException(String message, Throwable cause) { super(message, cause); }
}

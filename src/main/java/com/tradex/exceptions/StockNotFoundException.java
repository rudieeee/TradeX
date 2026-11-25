package com.tradex.exceptions;

public class StockNotFoundException extends TradeXException {
    public StockNotFoundException(String message) { super(message); }
    public StockNotFoundException() { super("Stock not found"); }
}

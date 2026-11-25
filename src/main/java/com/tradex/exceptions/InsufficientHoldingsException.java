package com.tradex.exceptions;

public class InsufficientHoldingsException extends TradeXException {
    public InsufficientHoldingsException(String message) { super(message); }
    public InsufficientHoldingsException() { super("Not enough shares to sell"); }
}

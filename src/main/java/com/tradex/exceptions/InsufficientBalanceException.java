package com.tradex.exceptions;

public class InsufficientBalanceException extends TradeXException {
    public InsufficientBalanceException(String message) { super(message); }
    public InsufficientBalanceException() { super("Insufficient wallet balance"); }
}

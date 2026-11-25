package com.tradex.exceptions;

public class InvalidOrderException extends TradeXException {
    public InvalidOrderException(String message) { super(message); }
    public InvalidOrderException() { super("Invalid order"); }
}

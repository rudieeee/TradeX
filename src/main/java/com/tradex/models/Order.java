package com.tradex.models;

import com.tradex.exceptions.TradeXException;
import com.tradex.repositories.HoldingRepository;
import com.tradex.repositories.WalletRepository;

import java.math.BigDecimal;
import java.sql.Connection;

/** A market order. Subclasses define how execution changes the wallet and holdings (polymorphism). */
public abstract class Order {
    protected final long userId;
    protected final String symbol;
    protected final int quantity;

    protected Order(long userId, String symbol, int quantity) {
        this.userId = userId;
        this.symbol = symbol;
        this.quantity = quantity;
    }

    public long getUserId() { return userId; }
    public String getSymbol() { return symbol; }
    public int getQuantity() { return quantity; }
    public abstract Side getSide();

    /** Applies the trade at {@code price} using the caller's open DB transaction. */
    public abstract void execute(Connection c, BigDecimal price, WalletRepository wallets, HoldingRepository holdings)
            throws TradeXException;

    protected BigDecimal total(BigDecimal price) { return price.multiply(BigDecimal.valueOf(quantity)); }
}

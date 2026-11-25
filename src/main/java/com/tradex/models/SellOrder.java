package com.tradex.models;

import com.tradex.exceptions.*;
import com.tradex.repositories.HoldingRepository;
import com.tradex.repositories.WalletRepository;

import java.math.BigDecimal;
import java.sql.Connection;

public class SellOrder extends Order {
    public SellOrder(long userId, String symbol, int quantity) { super(userId, symbol, quantity); }

    @Override public Side getSide() { return Side.SELL; }

    @Override
    public void execute(Connection c, BigDecimal price, WalletRepository wallets, HoldingRepository holdings)
            throws TradeXException {
        Holding h = holdings.lock(c, userId, symbol)
                .orElseThrow(() -> new InsufficientHoldingsException("You do not own " + symbol));
        if (h.quantity() < quantity) {
            throw new InsufficientHoldingsException("You own " + h.quantity() + " of " + symbol + ", cannot sell " + quantity);
        }
        BigDecimal balance = wallets.lockBalance(c, userId);
        wallets.setBalance(c, userId, balance.add(total(price)));
        if (h.quantity() == quantity) holdings.delete(c, userId, symbol);
        else holdings.update(c, userId, symbol, h.quantity() - quantity, h.avgPrice());
    }
}

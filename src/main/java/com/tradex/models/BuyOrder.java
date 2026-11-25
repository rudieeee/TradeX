package com.tradex.models;

import com.tradex.exceptions.*;
import com.tradex.repositories.HoldingRepository;
import com.tradex.repositories.WalletRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;

public class BuyOrder extends Order {
    public BuyOrder(long userId, String symbol, int quantity) { super(userId, symbol, quantity); }

    @Override public Side getSide() { return Side.BUY; }

    @Override
    public void execute(Connection c, BigDecimal price, WalletRepository wallets, HoldingRepository holdings)
            throws TradeXException {
        BigDecimal cost = total(price);
        BigDecimal balance = wallets.lockBalance(c, userId);
        if (balance.compareTo(cost) < 0) {
            throw new InsufficientBalanceException("Need ₹" + cost + " but wallet has ₹" + balance);
        }
        wallets.setBalance(c, userId, balance.subtract(cost));

        var existing = holdings.lock(c, userId, symbol);
        if (existing.isPresent()) {
            Holding h = existing.get();
            int newQty = h.quantity() + quantity;
            BigDecimal newAvg = h.avgPrice().multiply(BigDecimal.valueOf(h.quantity())).add(cost)
                    .divide(BigDecimal.valueOf(newQty), 2, RoundingMode.HALF_UP);
            holdings.update(c, userId, symbol, newQty, newAvg);
        } else {
            holdings.insert(c, userId, symbol, quantity, price);
        }
    }
}

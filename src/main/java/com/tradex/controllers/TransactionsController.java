package com.tradex.controllers;

import com.tradex.models.Side;
import com.tradex.models.Transaction;
import com.tradex.services.*;
import com.tradex.utils.*;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.format.DateTimeFormatter;

public class TransactionsController {
    @FXML private TableView<Transaction> table;
    @FXML private Label message;

    private final TradingService trading = new TradingServiceImpl();

    @FXML
    private void initialize() {
        Tables.fit(table);
        table.setPlaceholder(new Label("No trades yet. Place your first order from the Market tab."));
        DateTimeFormatter f = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
        Tables.text(table, "Time", t -> f.format(t.executedAt()));
        Tables.text(table, "Symbol", Transaction::symbol);
        Tables.signed(table, "Side", t -> t.side().name(), t -> t.side() == Side.BUY ? 1 : -1);
        Tables.text(table, "Qty", t -> String.valueOf(t.quantity()));
        Tables.text(table, "Price", t -> Fmt.money(t.price()));
        Tables.text(table, "Total", t -> Fmt.money(t.total()));
        long uid = Session.getCurrentUser().getId();
        Async.run(() -> trading.getTransactions(uid), list -> table.setItems(FXCollections.observableArrayList(list)),
                m -> message.setText(m));
    }
}

package com.tradex.controllers;

import com.tradex.models.Quote;
import com.tradex.services.*;
import com.tradex.utils.*;
import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class WatchlistController {
    @FXML private TableView<Quote> table;
    @FXML private Button removeButton;
    @FXML private Label message;

    private final MarketService market = new MarketService();
    private final WatchlistService watchlist = new WatchlistService();
    private final long uid = Session.getCurrentUser().getId();

    @FXML
    private void initialize() {
        Tables.fit(table);
        table.setPlaceholder(new Label("Your watchlist is empty. Add stocks from the Market tab."));
        Tables.text(table, "Symbol", Quote::symbol);
        Tables.text(table, "Name", q -> q.stock().name());
        Tables.text(table, "Price", q -> Fmt.money(q.price()));
        Tables.signed(table, "Change", q -> Fmt.pct(q.changePct()), q -> q.change().signum());
        removeButton.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        PriceFeed.tickProperty().addListener(new WeakInvalidationListener(tickListener));
        refresh();
    }

    private final InvalidationListener tickListener = o -> refresh();

    private void refresh() {
        Async.run(() -> {
            var symbols = watchlist.symbols(uid);
            return market.getQuotes().stream().filter(q -> symbols.contains(q.symbol())).toList();
        }, list -> table.setItems(FXCollections.observableArrayList(list)), m -> message.setText(m));
    }

    @FXML
    private void onRemove() {
        Quote q = table.getSelectionModel().getSelectedItem();
        if (q == null) return;
        Async.run(() -> { watchlist.remove(uid, q.symbol()); return true; }, ok -> refresh(), m -> message.setText(m));
    }
}

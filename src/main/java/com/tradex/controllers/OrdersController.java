package com.tradex.controllers;

import com.tradex.models.*;
import com.tradex.services.*;
import com.tradex.utils.*;
import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.format.DateTimeFormatter;

/** Limit and stop-loss orders: pending ones can be cancelled; executed/cancelled/rejected stay for reference. */
public class OrdersController {
    @FXML private TableView<OrderRecord> table;
    @FXML private Button cancelButton;
    @FXML private Label message;

    private final TradingService trading = new TradingServiceImpl();
    private final long uid = Session.getCurrentUser().getId();
    private final InvalidationListener tickListener = o -> refresh();

    @FXML
    private void initialize() {
        Tables.fit(table);
        table.setPlaceholder(new Label("No orders yet. Choose Limit or Stop-loss on the Market tab."));
        DateTimeFormatter f = DateTimeFormatter.ofPattern("dd MMM, HH:mm");
        Tables.text(table, "Placed", o -> f.format(o.createdAt()));
        Tables.text(table, "Symbol", OrderRecord::symbol);
        Tables.signed(table, "Side", o -> o.side().name(), o -> o.side() == Side.BUY ? 1 : -1);
        Tables.text(table, "Type", o -> o.type() == OrderType.STOP_LOSS ? "Stop-loss" : "Limit");
        Tables.text(table, "Qty", o -> String.valueOf(o.quantity()));
        Tables.text(table, "Trigger", o -> o.triggerPrice() == null ? "–" : Fmt.money(o.triggerPrice()));
        Tables.signed(table, "Status", o -> o.status().name(), o -> o.status() == OrderStatus.EXECUTED ? 1
                : o.status() == OrderStatus.REJECTED ? -1 : 0);
        cancelButton.disableProperty().bind(javafx.beans.binding.Bindings.createBooleanBinding(() -> {
            OrderRecord o = table.getSelectionModel().getSelectedItem();
            return o == null || o.status() != OrderStatus.PENDING;
        }, table.getSelectionModel().selectedItemProperty()));
        PriceFeed.tickProperty().addListener(new WeakInvalidationListener(tickListener));
        refresh();
    }

    private void refresh() {
        Async.run(() -> trading.getOrders(uid), list -> {
            OrderRecord sel = table.getSelectionModel().getSelectedItem();
            table.setItems(FXCollections.observableArrayList(list));
            if (sel != null) list.stream().filter(o -> o.id() == sel.id()).findFirst().ifPresent(o -> table.getSelectionModel().select(o));
        }, m -> message.setText(m));
    }

    @FXML
    private void onCancel() {
        OrderRecord o = table.getSelectionModel().getSelectedItem();
        if (o == null) return;
        Async.run(() -> { trading.cancelOrder(uid, o.id()); return true; }, ok -> refresh(), m -> { message.setText(m); refresh(); });
    }
}

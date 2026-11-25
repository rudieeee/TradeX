package com.tradex.controllers;

import com.tradex.models.Side;
import com.tradex.models.Transaction;
import com.tradex.services.*;
import com.tradex.utils.*;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

import java.time.format.DateTimeFormatter;

public class TransactionsController {
    @FXML private TableView<Transaction> table;
    @FXML private Label message;

    private java.util.List<Transaction> loaded = java.util.List.of();
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
        Async.run(() -> trading.getTransactions(uid), list -> { loaded = list; table.setItems(FXCollections.observableArrayList(list)); },
                m -> message.setText(m));
    }

    @FXML
    private void onExport() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Export transactions");
        fc.setInitialFileName("tradex-transactions.csv");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
        java.io.File f = fc.showSaveDialog(table.getScene().getWindow());
        if (f == null) return;
        var snapshot = loaded;
        Async.run(() -> { ReportExporter.exportTransactions(snapshot, f.toPath()); return snapshot.size(); },
                n -> { message.getStyleClass().setAll("label", "pos"); message.setText("Exported " + n + " transactions to " + f.getName()); },
                m -> { message.getStyleClass().setAll("label", "error"); message.setText("Export failed: " + m); });
    }
}

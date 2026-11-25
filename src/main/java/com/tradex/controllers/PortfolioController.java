package com.tradex.controllers;

import com.tradex.models.PortfolioRow;
import com.tradex.services.*;
import com.tradex.utils.*;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;

public class PortfolioController {
    @FXML private TableView<PortfolioRow> table;
    @FXML private Label investedLabel, valueLabel, pnlLabel, message;
    @FXML private PieChart allocation;
    @FXML private TextField sellQty;
    @FXML private Button sellButton;

    private final PortfolioService portfolio = new PortfolioService();
    private final TradingService trading = new TradingServiceImpl();
    private final long uid = Session.getCurrentUser().getId();

    @FXML
    private void initialize() {
        Tables.fit(table);
        Tables.text(table, "Symbol", PortfolioRow::symbol);
        Tables.text(table, "Qty", r -> String.valueOf(r.quantity()));
        Tables.text(table, "Avg price", r -> Fmt.money(r.avgPrice()));
        Tables.text(table, "LTP", r -> Fmt.money(r.ltp()));
        Tables.text(table, "Value", r -> Fmt.money(r.value()));
        Tables.signed(table, "P&L", r -> Fmt.signedMoney(r.pnl()), r -> r.pnl().signum());
        Tables.signed(table, "Return", r -> Fmt.pct(r.pnlPct()), r -> r.pnl().signum());
        sellButton.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        refresh();
    }

    private void refresh() {
        Async.run(() -> portfolio.getSummary(uid), s -> {
            table.setItems(FXCollections.observableArrayList(s.rows()));
            investedLabel.setText(Fmt.money(s.invested()));
            valueLabel.setText(Fmt.money(s.marketValue()));
            pnlLabel.setText(Fmt.signedMoney(s.pnl()) + "  (" + Fmt.pct(s.pnlPct()) + ")");
            pnlLabel.getStyleClass().removeAll("pos", "neg");
            if (s.pnl().signum() != 0) pnlLabel.getStyleClass().add(s.pnl().signum() > 0 ? "pos" : "neg");
            var slices = FXCollections.<PieChart.Data>observableArrayList();
            s.rows().forEach(r -> slices.add(new PieChart.Data(r.symbol(), r.value().doubleValue())));
            allocation.setData(slices);
        }, this::fail);
    }

    /** Sells the typed quantity of the selected holding, or all of it if the box is empty. */
    @FXML
    private void onSell() {
        PortfolioRow row = table.getSelectionModel().getSelectedItem();
        if (row == null) return;
        int q;
        try {
            String t = sellQty.getText().trim();
            q = t.isEmpty() ? row.quantity() : Integer.parseInt(t);
        } catch (NumberFormatException e) { fail("Enter a whole number of shares"); return; }
        sellButton.disableProperty().unbind();
        sellButton.setDisable(true);
        Async.run(() -> { trading.sell(uid, row.symbol(), q); return trading.getBalance(uid); }, bal -> {
            Session.balanceProperty().set(bal);
            message.getStyleClass().setAll("label", "pos");
            message.setText("Sold " + q + " × " + row.symbol());
            sellQty.clear();
            sellButton.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
            refresh();
        }, m -> {
            fail(m);
            sellButton.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        });
    }

    private void fail(String m) { message.getStyleClass().setAll("label", "error"); message.setText(m); }
}

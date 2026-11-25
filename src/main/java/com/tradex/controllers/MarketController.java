package com.tradex.controllers;

import com.tradex.models.*;
import com.tradex.services.*;
import com.tradex.utils.*;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Supplier;

public class MarketController {
    @FXML private TextField search, qty;
    @FXML private TableView<Quote> table;
    @FXML private Label nameLabel, priceLabel, changeLabel, message;
    @FXML private LineChart<String, Number> chart;
    @FXML private NumberAxis yAxis;
    @FXML private Button buyButton, sellButton, watchButton;

    private final MarketService market = new MarketService();
    private final TradingService trading = new TradingServiceImpl();
    private final WatchlistService watchlist = new WatchlistService();
    private final long uid = Session.getCurrentUser().getId();
    private final Set<String> watched = new HashSet<>();
    private FilteredList<Quote> filtered;
    private Quote selected;

    @FXML
    private void initialize() {
        Tables.fit(table);
        Tables.text(table, "Symbol", Quote::symbol);
        Tables.text(table, "Name", q -> q.stock().name());
        Tables.text(table, "Price", q -> Fmt.money(q.price()));
        Tables.signed(table, "Change", q -> Fmt.pct(q.changePct()), q -> q.change().signum());
        yAxis.setForceZeroInRange(false);
        chart.setCreateSymbols(false);
        chart.setAnimated(false);
        chart.setLegendVisible(false);
        setTradeEnabled(false);

        table.getSelectionModel().selectedItemProperty().addListener((o, a, q) -> { if (q != null) select(q); });
        Async.run(() -> { watched.addAll(watchlist.symbols(uid)); return market.getQuotes(); }, quotes -> {
            filtered = new FilteredList<>(FXCollections.observableArrayList(quotes));
            table.setItems(filtered);
            search.textProperty().addListener((obs, old, text) -> {
                String t = text.trim().toLowerCase();
                filtered.setPredicate(q -> t.isEmpty() || q.symbol().toLowerCase().contains(t)
                        || q.stock().name().toLowerCase().contains(t));
            });
            if (!quotes.isEmpty()) table.getSelectionModel().selectFirst();
        }, this::fail);
    }

    private void select(Quote q) {
        selected = q;
        nameLabel.setText(q.symbol() + " · " + q.stock().name());
        priceLabel.setText(Fmt.money(q.price()));
        changeLabel.setText(Fmt.signedMoney(q.change()) + "  (" + Fmt.pct(q.changePct()) + ")");
        changeLabel.getStyleClass().removeAll("pos", "neg");
        if (q.change().signum() != 0) changeLabel.getStyleClass().add(q.change().signum() > 0 ? "pos" : "neg");
        updateWatchButton();
        setTradeEnabled(true);
        message.setText("");
        Async.run(() -> market.getHistory(q.symbol(), q.stock()), pts -> {
            if (selected != q) return; // user moved on
            var series = new XYChart.Series<String, Number>();
            DateTimeFormatter f = DateTimeFormatter.ofPattern("dd MMM");
            for (PricePoint p : pts) series.getData().add(new XYChart.Data<>(f.format(p.time()), p.price()));
            chart.getData().setAll(List.of(series));
        }, this::fail);
    }

    @FXML private void onBuy() { trade(() -> trading.buy(uid, selected.symbol(), parseQty()), "Bought"); }
    @FXML private void onSell() { trade(() -> trading.sell(uid, selected.symbol(), parseQty()), "Sold"); }

    private interface TradeCall { Transaction call() throws Exception; }

    private void trade(TradeCall call, String verb) {
        final int q;
        try { q = parseQty(); } catch (NumberFormatException e) { fail("Enter a whole number of shares"); return; }
        setTradeEnabled(false);
        Async.run(() -> { Transaction t = call.call(); return new Object[]{t, trading.getBalance(uid)}; }, r -> {
            Transaction t = (Transaction) r[0];
            Session.balanceProperty().set((java.math.BigDecimal) r[1]);
            ok(verb + " " + t.quantity() + " × " + t.symbol() + " at " + Fmt.money(t.price()));
            setTradeEnabled(true);
        }, msg -> { fail(msg); setTradeEnabled(true); });
    }

    private int parseQty() { return Integer.parseInt(qty.getText().trim()); }

    @FXML
    private void onToggleWatch() {
        if (selected == null) return;
        String sym = selected.symbol();
        boolean adding = !watched.contains(sym);
        Async.run(() -> { if (adding) watchlist.add(uid, sym); else watchlist.remove(uid, sym); return adding; }, a -> {
            if (a) watched.add(sym); else watched.remove(sym);
            updateWatchButton();
        }, this::fail);
    }

    private void updateWatchButton() {
        watchButton.setText(selected != null && watched.contains(selected.symbol()) ? "★ In watchlist" : "☆ Add to watchlist");
    }

    private void setTradeEnabled(boolean on) {
        buyButton.setDisable(!on);
        sellButton.setDisable(!on);
        watchButton.setDisable(selected == null);
    }

    private void ok(String m) { message.getStyleClass().setAll("label", "pos"); message.setText(m); }
    private void fail(String m) { message.getStyleClass().setAll("label", "error"); message.setText(m); }
}

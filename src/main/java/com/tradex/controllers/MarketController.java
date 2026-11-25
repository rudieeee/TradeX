package com.tradex.controllers;

import com.tradex.models.*;
import com.tradex.services.*;
import com.tradex.utils.*;
import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;

import java.time.format.DateTimeFormatter;
import java.util.*;

public class MarketController {
    @FXML private TextField search, qty, priceField;
    @FXML private ChoiceBox<OrderType> orderType;
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
    private final ObservableList<Quote> base = FXCollections.observableArrayList();
    private FilteredList<Quote> filtered;
    private final InvalidationListener tickListener = o -> reload();
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
        orderType.setItems(FXCollections.observableArrayList(OrderType.values()));
        orderType.setValue(OrderType.MARKET);
        priceField.visibleProperty().bind(orderType.valueProperty().isNotEqualTo(OrderType.MARKET));
        priceField.managedProperty().bind(priceField.visibleProperty());
        orderType.valueProperty().addListener((o, a, t) ->
                priceField.setPromptText(t == OrderType.STOP_LOSS ? "Trigger price (sell)" : "Limit price"));
        setTradeEnabled(false);

        table.getSelectionModel().selectedItemProperty().addListener((o, a, q) -> { if (q != null) select(q); });
        filtered = new FilteredList<>(base);
        table.setItems(filtered);
        search.textProperty().addListener((obs, old, text) -> {
            String t = text.trim().toLowerCase();
            filtered.setPredicate(q -> t.isEmpty() || q.symbol().toLowerCase().contains(t)
                    || q.stock().name().toLowerCase().contains(t));
        });
        PriceFeed.tickProperty().addListener(new WeakInvalidationListener(tickListener));
        Async.run(() -> { watched.addAll(watchlist.symbols(uid)); return market.getQuotes(); }, quotes -> {
            base.setAll(quotes);
            if (!quotes.isEmpty()) table.getSelectionModel().selectFirst();
        }, this::fail);
    }

    /** Refreshes prices after a background update, keeping the current selection. */
    private void reload() {
        String keep = selected == null ? null : selected.symbol();
        Async.run(market::getQuotes, quotes -> {
            base.setAll(quotes);
            if (keep != null) quotes.stream().filter(q -> q.symbol().equals(keep)).findFirst()
                    .ifPresent(q -> table.getSelectionModel().select(q));
        }, this::fail);
    }

    private void select(Quote q) {
        boolean sameStock = selected != null && selected.symbol().equals(q.symbol());
        selected = q;
        nameLabel.setText(q.symbol() + " · " + q.stock().name());
        priceLabel.setText(Fmt.money(q.price()));
        changeLabel.setText(Fmt.signedMoney(q.change()) + "  (" + Fmt.pct(q.changePct()) + ")");
        changeLabel.getStyleClass().removeAll("pos", "neg");
        if (q.change().signum() != 0) changeLabel.getStyleClass().add(q.change().signum() > 0 ? "pos" : "neg");
        updateWatchButton();
        setTradeEnabled(true);
        if (!sameStock) message.setText("");
        Async.run(() -> market.getHistory(q.symbol(), q.stock()), pts -> {
            if (selected != q) return; // user moved on
            var series = new XYChart.Series<String, Number>();
            DateTimeFormatter f = DateTimeFormatter.ofPattern("dd MMM");
            for (PricePoint p : pts) series.getData().add(new XYChart.Data<>(f.format(p.time()), p.price()));
            chart.getData().setAll(List.of(series));
        }, this::fail);
    }

    @FXML private void onBuy() { submit(Side.BUY); }
    @FXML private void onSell() { submit(Side.SELL); }

    /** Market orders execute now; limit and stop-loss orders are queued until the price condition is met. */
    private void submit(Side side) {
        if (selected == null) return;
        final int q;
        try { q = parseQty(); } catch (NumberFormatException e) { fail("Enter a whole number of shares"); return; }
        final String sym = selected.symbol();
        final OrderType type = orderType.getValue();
        final java.math.BigDecimal trigger;
        if (type == OrderType.MARKET) {
            trigger = null;
        } else {
            try { trigger = new java.math.BigDecimal(priceField.getText().trim()); }
            catch (NumberFormatException e) { fail("Enter a valid price"); return; }
        }
        setTradeEnabled(false);
        Async.run(() -> {
            if (type == OrderType.MARKET) {
                Transaction t = side == Side.BUY ? trading.buy(uid, sym, q) : trading.sell(uid, sym, q);
                return new Object[]{"%s %d × %s at %s".formatted(side == Side.BUY ? "Bought" : "Sold", q, sym, Fmt.money(t.price())),
                        trading.getBalance(uid)};
            }
            OrderRecord o = trading.placeConditionalOrder(uid, sym, side, type, q, trigger);
            String what = (type == OrderType.LIMIT ? "Limit " : "Stop-loss ") + side.name().toLowerCase();
            String state = o.status() == OrderStatus.EXECUTED ? " (filled immediately)" : " (pending, see Orders)";
            return new Object[]{"%s order placed: %d × %s @ %s%s".formatted(what, q, sym, Fmt.money(o.triggerPrice()), state),
                    trading.getBalance(uid)};
        }, r -> {
            Session.balanceProperty().set((java.math.BigDecimal) r[1]);
            ok((String) r[0]);
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

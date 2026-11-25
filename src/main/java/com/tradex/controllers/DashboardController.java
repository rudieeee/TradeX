package com.tradex.controllers;

import com.tradex.models.Quote;
import com.tradex.services.MarketService;
import com.tradex.services.PortfolioService;
import com.tradex.utils.*;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;

import java.util.Comparator;
import java.util.List;

public class DashboardController {
    @FXML private Label cashLabel, investedLabel, valueLabel, pnlLabel, error;
    @FXML private PieChart allocation;
    @FXML private TableView<Quote> trending;

    private final PortfolioService portfolio = new PortfolioService();
    private final MarketService market = new MarketService();

    @FXML
    private void initialize() {
        Tables.fit(trending);
        Tables.text(trending, "Symbol", Quote::symbol);
        Tables.text(trending, "Price", q -> Fmt.money(q.price()));
        Tables.signed(trending, "Change", q -> Fmt.pct(q.changePct()), q -> q.change().signum());

        long uid = Session.getCurrentUser().getId();
        Async.run(() -> portfolio.getSummary(uid), s -> {
            cashLabel.setText(Fmt.money(s.cash()));
            investedLabel.setText(Fmt.money(s.invested()));
            valueLabel.setText(Fmt.money(s.totalValue()));
            pnlLabel.setText(Fmt.signedMoney(s.pnl()) + "  (" + Fmt.pct(s.pnlPct()) + ")");
            pnlLabel.getStyleClass().removeAll("pos", "neg");
            if (s.pnl().signum() != 0) pnlLabel.getStyleClass().add(s.pnl().signum() > 0 ? "pos" : "neg");
            var slices = FXCollections.<PieChart.Data>observableArrayList();
            s.rows().forEach(r -> slices.add(new PieChart.Data(r.symbol(), r.value().doubleValue())));
            if (s.cash().signum() > 0) slices.add(new PieChart.Data("Cash", s.cash().doubleValue()));
            allocation.setData(slices);
        }, this::fail);

        Async.run(market::getQuotes, quotes -> {
            List<Quote> movers = quotes.stream()
                    .sorted(Comparator.comparing((Quote q) -> q.changePct().abs()).reversed()).limit(8).toList();
            trending.setItems(FXCollections.observableArrayList(movers));
        }, this::fail);
    }

    private void fail(String msg) { error.setText(msg); error.setVisible(true); }
}

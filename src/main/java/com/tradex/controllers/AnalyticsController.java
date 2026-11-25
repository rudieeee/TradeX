package com.tradex.controllers;

import com.tradex.models.PortfolioRow;
import com.tradex.models.PricePoint;
import com.tradex.services.AnalyticsService;
import com.tradex.utils.*;
import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.Label;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class AnalyticsController {
    @FXML private Label returnLabel, realizedLabel, unrealizedLabel, winRateLabel, tradesLabel, message;
    @FXML private LineChart<String, Number> valueChart;
    @FXML private NumberAxis valueAxis;
    @FXML private BarChart<String, Number> pnlChart;

    private final AnalyticsService service = new AnalyticsService();
    private final long uid = Session.getCurrentUser().getId();
    private final InvalidationListener tickListener = o -> refresh();

    @FXML
    private void initialize() {
        valueAxis.setForceZeroInRange(false);
        valueChart.setCreateSymbols(false);
        valueChart.setAnimated(false);
        valueChart.setLegendVisible(false);
        pnlChart.setAnimated(false);
        pnlChart.setLegendVisible(false);
        PriceFeed.tickProperty().addListener(new WeakInvalidationListener(tickListener));
        refresh();
    }

    private void refresh() {
        Async.run(() -> service.getAnalytics(uid), a -> {
            setSigned(returnLabel, Fmt.pct(a.totalReturnPct()), a.totalReturnPct().signum());
            setSigned(realizedLabel, Fmt.signedMoney(a.stats().realizedPnl()), a.stats().realizedPnl().signum());
            setSigned(unrealizedLabel, Fmt.signedMoney(a.summary().pnl()), a.summary().pnl().signum());
            winRateLabel.setText(a.stats().sells() == 0 ? "–" : a.stats().winRatePercent() + "%  (" + a.stats().winningSells() + "/" + a.stats().sells() + " sells)");
            tradesLabel.setText(String.valueOf(a.stats().trades()));

            var line = new XYChart.Series<String, Number>();
            DateTimeFormatter f = DateTimeFormatter.ofPattern("dd MMM HH:mm");
            for (PricePoint p : a.valueHistory()) line.getData().add(new XYChart.Data<>(f.format(p.time()), p.price()));
            valueChart.getData().setAll(List.of(line));

            var bars = new XYChart.Series<String, Number>();
            for (PortfolioRow r : a.summary().rows()) bars.getData().add(new XYChart.Data<>(r.symbol(), r.pnl()));
            pnlChart.getData().setAll(List.of(bars));
        }, m -> message.setText(m));
    }

    private void setSigned(Label l, String text, int sign) {
        l.setText(text);
        l.getStyleClass().removeAll("pos", "neg");
        if (sign != 0) l.getStyleClass().add(sign > 0 ? "pos" : "neg");
    }
}

package com.tradex.controllers;

import com.tradex.Main;
import com.tradex.models.User;
import com.tradex.services.TradingService;
import com.tradex.services.TradingServiceImpl;
import com.tradex.utils.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.Node;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

/** Main window: sidebar navigation, wallet header and the swappable content area. */
public class ShellController {
    @FXML private BorderPane content;
    @FXML private Label userLabel, balanceLabel;
    @FXML private Button navDashboard, navMarket, navPortfolio, navWatchlist, navTransactions;

    @FXML
    private void initialize() {
        User u = Session.getCurrentUser();
        userLabel.setText(u.getUsername());
        balanceLabel.textProperty().bind(Session.balanceProperty().map(Fmt::money));
        TradingService trading = new TradingServiceImpl();
        Async.run(() -> trading.getBalance(u.getId()), b -> Session.balanceProperty().set(b), msg -> {});
        show("dashboard", navDashboard);
    }

    @FXML private void onDashboard() { show("dashboard", navDashboard); }
    @FXML private void onMarket() { show("market", navMarket); }
    @FXML private void onPortfolio() { show("portfolio", navPortfolio); }
    @FXML private void onWatchlist() { show("watchlist", navWatchlist); }
    @FXML private void onTransactions() { show("transactions", navTransactions); }

    @FXML
    private void onLogout() {
        balanceLabel.textProperty().unbind();
        Session.clear();
        Main.showLogin();
    }

    private void show(String view, Button active) {
        for (Button b : List.of(navDashboard, navMarket, navPortfolio, navWatchlist, navTransactions)) {
            b.getStyleClass().remove("active");
        }
        active.getStyleClass().add("active");
        try {
            Node n = FXMLLoader.load(getClass().getResource("/fxml/" + view + ".fxml"));
            content.setCenter(n);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load view " + view, e);
        }
    }
}

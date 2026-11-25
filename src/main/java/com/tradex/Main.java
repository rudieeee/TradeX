package com.tradex;

import com.tradex.repositories.PriceHistoryRepository;
import com.tradex.repositories.StockRepository;
import com.tradex.services.*;
import com.tradex.utils.PriceFeed;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.UncheckedIOException;

public class Main extends Application {
    private static Stage stage;
    private PriceUpdater updater;

    @Override
    public void start(Stage primaryStage) {
        stage = primaryStage;
        stage.setTitle("TradeX");
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            System.err.println("Uncaught error in " + t.getName() + ": " + e);
            e.printStackTrace();
        });
        // Fetches live prices every minute (simulated per symbol when unavailable) and seeds history on first run.
        updater = new PriceUpdater(new YahooFinanceProvider(), new StockRepository(), new PriceHistoryRepository(), new MarketService());
        updater.start(60, r -> Platform.runLater(() -> PriceFeed.publish(
                r.isLive() ? (r.simulated() == 0 ? "● Live prices" : "● Live prices (" + r.simulated() + " simulated)")
                           : "● Simulated prices")));
        showLogin();
        stage.show();
    }

    @Override
    public void stop() { if (updater != null) updater.stop(); }

    public static void showLogin() { setScene("/fxml/login.fxml", 460, 560); }
    public static void showHome() { setScene("/fxml/shell.fxml", 1180, 720); }

    private static void setScene(String fxml, double w, double h) {
        try {
            Parent root = FXMLLoader.load(Main.class.getResource(fxml));
            Scene scene = new Scene(root, w, h);
            scene.getStylesheets().add(Main.class.getResource("/css/style.css").toExternalForm());
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load " + fxml, e);
        }
    }

    public static void main(String[] args) { launch(args); }
}

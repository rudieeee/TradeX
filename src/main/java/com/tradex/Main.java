package com.tradex;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.UncheckedIOException;

public class Main extends Application {
    private static Stage stage;

    @Override
    public void start(Stage primaryStage) {
        stage = primaryStage;
        stage.setTitle("TradeX");
        showLogin();
        stage.show();
    }

    public static void showLogin() { setScene("/fxml/login.fxml", 460, 560); }
    public static void showHome() { setScene("/fxml/home.fxml", 900, 600); }

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

package com.tradex.controllers;

import com.tradex.Main;
import com.tradex.models.User;
import com.tradex.utils.Session;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

/** Placeholder landing screen; replaced by the dashboard in Phase 3. */
public class HomeController {
    @FXML private Label welcome;

    @FXML
    private void initialize() {
        User u = Session.getCurrentUser();
        welcome.setText("Welcome, " + u.getUsername() + "!");
    }

    @FXML
    private void onLogout() {
        Session.clear();
        Main.showLogin();
    }
}

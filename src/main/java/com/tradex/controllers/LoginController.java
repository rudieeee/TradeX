package com.tradex.controllers;

import com.tradex.Main;
import com.tradex.exceptions.TradeXException;
import com.tradex.models.User;
import com.tradex.repositories.UserRepository;
import com.tradex.services.AuthService;
import com.tradex.utils.Session;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class LoginController {
    @FXML private TextField loginUser, regUser, regEmail;
    @FXML private PasswordField loginPass, regPass, regConfirm;
    @FXML private Label loginError, regError;
    @FXML private TabPane tabs;
    @FXML private Button loginButton, registerButton;

    private final AuthService auth = new AuthService(new UserRepository());

    @FXML
    private void onLogin() {
        run(loginButton, loginError, () -> auth.login(loginUser.getText(), loginPass.getText()));
    }

    @FXML
    private void onRegister() {
        if (!regPass.getText().equals(regConfirm.getText())) {
            show(regError, "Passwords do not match");
            return;
        }
        run(registerButton, regError, () -> auth.register(regUser.getText(), regEmail.getText(), regPass.getText()));
    }

    private interface Action { User call() throws TradeXException; }

    /** Runs the DB/hash work off the FX thread, then enters the app on success. */
    private void run(Button button, Label error, Action action) {
        error.setVisible(false);
        button.setDisable(true);
        Task<User> task = new Task<>() {
            @Override protected User call() throws Exception { return action.call(); }
        };
        task.setOnSucceeded(e -> {
            Session.setCurrentUser(task.getValue());
            Main.showHome();
        });
        task.setOnFailed(e -> {
            button.setDisable(false);
            Throwable t = task.getException();
            show(error, t instanceof TradeXException ? t.getMessage() : "Something went wrong. Please try again.");
            if (!(t instanceof TradeXException)) t.printStackTrace();
        });
        Thread th = new Thread(task, "auth-task");
        th.setDaemon(true);
        th.start();
    }

    private void show(Label l, String msg) {
        l.setText(msg);
        l.setVisible(true);
    }
}

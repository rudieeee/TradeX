package com.tradex.controllers;

import com.tradex.models.AdminUserRow;
import com.tradex.models.User;
import com.tradex.services.AdminService;
import com.tradex.utils.*;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.format.DateTimeFormatter;

/** Admin panel: manage users and monitor trading activity. The service enforces the admin check. */
public class AdminController {
    @FXML private TableView<AdminUserRow> users;
    @FXML private TableView<String[]> activity;
    @FXML private Button resetButton, deleteButton;
    @FXML private Label message;

    private final AdminService admin = new AdminService();
    private final User me = Session.getCurrentUser();

    @FXML
    private void initialize() {
        Tables.fit(users);
        Tables.text(users, "ID", u -> String.valueOf(u.id()));
        Tables.text(users, "Username", AdminUserRow::username);
        Tables.text(users, "Email", AdminUserRow::email);
        Tables.text(users, "Role", u -> u.role().name());
        Tables.text(users, "Joined", u -> DateTimeFormatter.ofPattern("dd MMM yyyy").format(u.createdAt()));
        Tables.text(users, "Cash", u -> Fmt.money(u.balance()));
        Tables.text(users, "Trades", u -> String.valueOf(u.trades()));
        var none = users.getSelectionModel().selectedItemProperty().isNull();
        resetButton.disableProperty().bind(none);
        deleteButton.disableProperty().bind(none);

        Tables.fit(activity);
        String[] heads = {"Time", "User", "Side", "Qty", "Symbol", "Price"};
        for (int i = 0; i < heads.length; i++) { final int idx = i; Tables.text(activity, heads[i], r -> r[idx]); }
        refresh();
    }

    private void refresh() {
        Async.run(() -> new Object[]{admin.listUsers(me), admin.recentActivity(me, 50)}, r -> {
            @SuppressWarnings("unchecked") var u = (java.util.List<AdminUserRow>) r[0];
            @SuppressWarnings("unchecked") var a = (java.util.List<String[]>) r[1];
            users.setItems(FXCollections.observableArrayList(u));
            activity.setItems(FXCollections.observableArrayList(a));
        }, m -> message.setText(m));
    }

    @FXML private void onReset() { confirmThen("Reset " + selectedName() + "? Their trades, holdings and watchlist are wiped and cash is restored to ₹1,00,000.",
            () -> admin.resetAccount(me, users.getSelectionModel().getSelectedItem().id())); }

    @FXML private void onDelete() { confirmThen("Permanently delete " + selectedName() + " and all their data?",
            () -> admin.deleteUser(me, users.getSelectionModel().getSelectedItem().id())); }

    private String selectedName() { return users.getSelectionModel().getSelectedItem().username(); }

    private interface AdminAction { void run() throws Exception; }

    private void confirmThen(String question, AdminAction action) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, question, ButtonType.OK, ButtonType.CANCEL);
        a.setHeaderText(null);
        if (a.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
        message.setText("");
        Async.run(() -> { action.run(); return true; }, ok -> refresh(), m -> { message.setText(m); refresh(); });
    }
}

package com.tradex.controllers;

import com.tradex.models.LeaderboardEntry;
import com.tradex.services.LeaderboardService;
import com.tradex.utils.*;
import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class LeaderboardController {
    @FXML private TableView<LeaderboardEntry> table;
    @FXML private Label message, yourRank;

    private final LeaderboardService service = new LeaderboardService();
    private final long uid = Session.getCurrentUser().getId();
    private final InvalidationListener tickListener = o -> refresh();

    @FXML
    private void initialize() {
        Tables.fit(table);
        Tables.text(table, "Rank", e -> "#" + e.rank());
        Tables.text(table, "Trader", e -> e.userId() == uid ? e.username() + " (you)" : e.username());
        Tables.text(table, "Portfolio value", e -> Fmt.money(e.totalValue()));
        Tables.signed(table, "Return", e -> Fmt.pct(e.returnPct()), e -> e.returnPct().signum());
        PriceFeed.tickProperty().addListener(new WeakInvalidationListener(tickListener));
        refresh();
    }

    private void refresh() {
        Async.run(service::getLeaderboard, list -> {
            table.setItems(FXCollections.observableArrayList(list));
            list.stream().filter(e -> e.userId() == uid).findFirst().ifPresentOrElse(
                    e -> yourRank.setText("Your rank: #" + e.rank() + " of " + list.size()),
                    () -> yourRank.setText("Admins are not ranked"));
        }, m -> message.setText(m));
    }
}

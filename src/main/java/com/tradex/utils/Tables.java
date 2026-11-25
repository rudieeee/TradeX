package com.tradex.utils;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.control.*;

import java.util.function.Function;
import java.util.function.ToIntFunction;

/** Small helpers for building TableView columns in code. */
public final class Tables {
    private Tables() {}

    public static <T> void fit(TableView<T> t) {
        t.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        t.setPlaceholder(new Label("Nothing to show yet"));
    }

    public static <T> TableColumn<T, String> text(TableView<T> t, String title, Function<T, String> f) {
        TableColumn<T, String> c = new TableColumn<>(title);
        c.setCellValueFactory(d -> new ReadOnlyStringWrapper(f.apply(d.getValue())));
        t.getColumns().add(c);
        return c;
    }

    /** Text column coloured green/red by the sign returned for the row. */
    public static <T> TableColumn<T, String> signed(TableView<T> t, String title, Function<T, String> f, ToIntFunction<T> sign) {
        TableColumn<T, String> c = text(t, title, f);
        c.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("pos", "neg");
                setText(empty ? null : item);
                if (!empty && getTableRow() != null && getTableRow().getItem() != null) {
                    @SuppressWarnings("unchecked") T row = (T) getTableRow().getItem();
                    int s = sign.applyAsInt(row);
                    if (s > 0) getStyleClass().add("pos");
                    else if (s < 0) getStyleClass().add("neg");
                }
            }
        });
        return c;
    }
}

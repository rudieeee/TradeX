package com.tradex.utils;

import com.tradex.exceptions.TradeXException;
import javafx.concurrent.Task;

import java.util.function.Consumer;

/** Runs blocking work (DB, network) off the JavaFX thread. Callbacks run on the FX thread. */
public final class Async {
    private Async() {}

    public interface Work<T> { T call() throws Exception; }

    public static <T> void run(Work<T> work, Consumer<T> onSuccess, Consumer<String> onError) {
        Task<T> task = new Task<>() {
            @Override protected T call() throws Exception { return work.call(); }
        };
        task.setOnSucceeded(e -> onSuccess.accept(task.getValue()));
        task.setOnFailed(e -> {
            Throwable t = task.getException();
            if (!(t instanceof TradeXException)) t.printStackTrace();
            onError.accept(t instanceof TradeXException ? t.getMessage() : "Something went wrong. Please try again.");
        });
        Thread th = new Thread(task, "tradex-async");
        th.setDaemon(true);
        th.start();
    }
}

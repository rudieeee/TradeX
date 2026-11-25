package com.tradex.utils;

import javafx.beans.property.*;

/** Observable state of the background price updater, so views can refresh when prices change. */
public final class PriceFeed {
    private static final LongProperty tick = new SimpleLongProperty();
    private static final StringProperty status = new SimpleStringProperty("Connecting…");

    private PriceFeed() {}

    /** Increments after every update. Listen with a WeakInvalidationListener to avoid leaking views. */
    public static ReadOnlyLongProperty tickProperty() { return tick; }
    public static ReadOnlyStringProperty statusProperty() { return status; }

    /** Call on the FX thread. */
    public static void publish(String statusText) {
        status.set(statusText);
        tick.set(tick.get() + 1);
    }
}

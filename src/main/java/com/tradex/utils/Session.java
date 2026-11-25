package com.tradex.utils;

import com.tradex.models.User;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.math.BigDecimal;

/** Holds the currently logged-in user for the JavaFX app. */
public final class Session {
    private static volatile User current;

    private static final ObjectProperty<BigDecimal> balance = new SimpleObjectProperty<>(BigDecimal.ZERO);

    private Session() {}

    /** Wallet balance shown in the header; update on the FX thread. */
    public static ObjectProperty<BigDecimal> balanceProperty() { return balance; }

    public static User getCurrentUser() { return current; }
    public static void setCurrentUser(User user) { current = user; }
    public static void clear() { current = null; }
}

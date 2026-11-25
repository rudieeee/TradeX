package com.tradex.utils;

import com.tradex.models.User;

/** Holds the currently logged-in user for the JavaFX app. */
public final class Session {
    private static volatile User current;

    private Session() {}

    public static User getCurrentUser() { return current; }
    public static void setCurrentUser(User user) { current = user; }
    public static void clear() { current = null; }
}

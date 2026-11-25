package com.tradex.database;

import com.tradex.exceptions.DataAccessException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Creates JDBC connections. Override defaults with TRADEX_DB_URL / TRADEX_DB_USER / TRADEX_DB_PASSWORD. */
public final class Database {
    private static final String URL = env("TRADEX_DB_URL",
            "jdbc:mysql://localhost:3306/tradex?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
    private static final String USER = env("TRADEX_DB_USER", "tradex");
    private static final String PASSWORD = env("TRADEX_DB_PASSWORD", "tradex");

    private Database() {}

    public static Connection getConnection() throws DataAccessException {
        try {
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (SQLException e) {
            throw new DataAccessException("Could not connect to the database", e);
        }
    }

    private static String env(String key, String def) {
        String v = System.getenv(key);
        return v == null || v.isBlank() ? def : v;
    }
}

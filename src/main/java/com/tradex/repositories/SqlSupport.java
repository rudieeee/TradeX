package com.tradex.repositories;

import com.tradex.exceptions.DataAccessException;

import java.sql.SQLException;

final class SqlSupport {
    private SqlSupport() {}
    static DataAccessException wrap(String msg, SQLException e) { return new DataAccessException(msg, e); }
}

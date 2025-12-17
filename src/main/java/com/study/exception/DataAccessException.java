package com.study.exception;

/**
 * Base exception for any database access failure.
 */
public class DataAccessException extends RbacException {
    public DataAccessException(String message) {
        super(message);
    }

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}

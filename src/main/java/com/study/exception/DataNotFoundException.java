package com.study.exception;

/**
 * Exception thrown when data is not found.
 */
public class DataNotFoundException extends DataAccessException {
    public DataNotFoundException(String message) {
        super(message);
    }
}

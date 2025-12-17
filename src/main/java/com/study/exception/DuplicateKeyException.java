package com.study.exception;

/**
 * Exception thrown when unique constraints are violated.
 */
public class DuplicateKeyException extends DataAccessException {
    public DuplicateKeyException(String message) {
        super(message);
    }
}

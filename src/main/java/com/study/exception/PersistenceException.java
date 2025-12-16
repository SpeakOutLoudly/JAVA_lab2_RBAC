package com.study.exception;

/**
 * Exception thrown when database operations fail
 */
public class PersistenceException extends RbacException {
    public PersistenceException(String message, Throwable cause) {
        super(message, cause);
    }
}

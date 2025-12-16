package com.study.exception;

/**
 * Exception thrown when data is not found
 */
public class DataNotFoundException extends RbacException {
    public DataNotFoundException(String message) {
        super(message);
    }
}

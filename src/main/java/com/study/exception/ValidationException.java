package com.study.exception;

/**
 * Exception thrown when validation fails
 */
public class ValidationException extends RbacException {
    public ValidationException(String message) {
        super(message);
    }
}

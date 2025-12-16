package com.study.exception;

/**
 * Exception thrown when duplicate data is encountered
 */
public class DuplicateDataException extends RbacException {
    public DuplicateDataException(String message) {
        super(message);
    }
}

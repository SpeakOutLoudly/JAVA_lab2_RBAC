package com.study.exception;

/**
 * Exception thrown when user lacks required permission
 */
public class PermissionDeniedException extends RbacException {
    public PermissionDeniedException(String message) {
        super(message);
    }
}

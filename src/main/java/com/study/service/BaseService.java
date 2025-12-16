package com.study.service;

import com.study.context.SessionContext;
import com.study.domain.AuditLog;
import com.study.exception.PermissionDeniedException;
import com.study.exception.ValidationException;
import com.study.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

/**
 * Base service with unified template: Authorization -> Validation -> Execution -> Audit
 */
public abstract class BaseService {
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final SessionContext sessionContext;
    protected final AuditLogRepository auditLogRepository;
    
    public BaseService(SessionContext sessionContext, AuditLogRepository auditLogRepository) {
        this.sessionContext = sessionContext;
        this.auditLogRepository = auditLogRepository;
    }
    
    /**
     * Execute business operation with full template
     */
    protected <T> T executeWithTemplate(
            String requiredPermission,
            String action,
            String resourceType,
            String resourceId,
            Runnable validation,
            Supplier<T> execution) {
        
        // Step 1: Authorization check
        if (requiredPermission != null) {
            checkPermission(requiredPermission, action);
        }
        
        // Step 2: Validation
        if (validation != null) {
            validation.run();
        }
        
        // Step 3: Execution and Audit
        try {
            T result = execution.get();
            
            // Step 4: Success audit
            auditSuccess(action, resourceType, resourceId, null);
            
            return result;
        } catch (Exception e) {
            // Step 4: Failure audit
            auditFailure(action, resourceType, resourceId, e.getMessage());
            throw e;
        }
    }
    
    /**
     * Execute without return value
     */
    protected void executeWithTemplate(
            String requiredPermission,
            String action,
            String resourceType,
            String resourceId,
            Runnable validation,
            Runnable execution) {
        
        executeWithTemplate(
            requiredPermission,
            action,
            resourceType,
            resourceId,
            validation,
            () -> {
                execution.run();
                return null;
            }
        );
    }
    
    /**
     * Check if current user has required permission
     */
    protected void checkPermission(String permissionCode, String action) {
        if (!sessionContext.isLoggedIn()) {
            String message = "Not logged in, cannot perform: " + action;
            logger.warn(message);
            throw new PermissionDeniedException(message);
        }
        
        if (!sessionContext.hasPermission(permissionCode)) {
            String message = String.format("Permission denied: %s (required: %s)", 
                    action, permissionCode);
            logger.warn("User {} attempted unauthorized action: {}", 
                    sessionContext.getCurrentUser().getUsername(), action);
            throw new PermissionDeniedException(message);
        }
    }
    
    /**
     * Validate not null
     */
    protected void validateNotNull(Object value, String fieldName) {
        if (value == null) {
            throw new ValidationException(fieldName + " cannot be null");
        }
    }
    
    /**
     * Validate not blank
     */
    protected void validateNotBlank(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(fieldName + " cannot be blank");
        }
    }
    
    /**
     * Audit successful operation
     */
    protected void auditSuccess(String action, String resourceType, String resourceId, String detail) {
        AuditLog log = new AuditLog(
                sessionContext.isLoggedIn() ? sessionContext.getCurrentUser().getId() : null,
                sessionContext.isLoggedIn() ? sessionContext.getCurrentUser().getUsername() : "anonymous",
                action,
                resourceType,
                resourceId,
                detail,
                true,
                null
        );
        auditLogRepository.save(log);
        logger.info("Audit: {} - {} - SUCCESS", action, resourceType);
    }
    
    /**
     * Audit failed operation
     */
    protected void auditFailure(String action, String resourceType, String resourceId, String errorMessage) {
        AuditLog log = new AuditLog(
                sessionContext.isLoggedIn() ? sessionContext.getCurrentUser().getId() : null,
                sessionContext.isLoggedIn() ? sessionContext.getCurrentUser().getUsername() : "anonymous",
                action,
                resourceType,
                resourceId,
                null,
                false,
                errorMessage
        );
        auditLogRepository.save(log);
        logger.warn("Audit: {} - {} - FAILED: {}", action, resourceType, errorMessage);
    }
}

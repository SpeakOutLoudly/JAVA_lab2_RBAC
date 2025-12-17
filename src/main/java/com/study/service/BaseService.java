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
    protected final Logger auditLogger = LoggerFactory.getLogger("com.study.audit");
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
            checkPermission(requiredPermission, action, resourceType, resourceId);
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
            logger.error("Failed to execute operation, action={}, resourceType={}, resourceId={}, error={}"
                    ,action, resourceType, resourceId, e.getMessage());

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
    protected void checkPermission(String permissionCode, String action, String resourceType, String resourceId) {
        if (!sessionContext.isLoggedIn()) {
            String message = "Not logged in, cannot perform: " + action;
            logger.warn(message);
            throw new PermissionDeniedException(message);
        }
        
        if (!sessionContext.hasPermission(permissionCode, resourceType, resourceId)) {
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
        String username = sessionContext.isLoggedIn() ? sessionContext.getCurrentUser().getUsername() : "anonymous";
        Long userId = sessionContext.isLoggedIn() ? sessionContext.getCurrentUser().getId() : null;
        
        AuditLog log = new AuditLog(
                userId,
                username,
                action,
                resourceType,
                resourceId,
                detail,
                true,
                null
        );
        auditLogRepository.save(log);
        
        // 审计日志：结构化输出到独立文件
        auditLogger.info("ACTION={} | RESOURCE_TYPE={} | RESOURCE_ID={} | USER={} | USER_ID={} | RESULT=SUCCESS",
                action, resourceType, resourceId != null ? resourceId : "N/A", username, userId);
    }
    
    /**
     * Audit failed operation
     */
    protected void auditFailure(String action, String resourceType, String resourceId, String errorMessage) {
        String username = sessionContext.isLoggedIn() ? sessionContext.getCurrentUser().getUsername() : "anonymous";
        Long userId = sessionContext.isLoggedIn() ? sessionContext.getCurrentUser().getId() : null;
        
        AuditLog log = new AuditLog(
                userId,
                username,
                action,
                resourceType,
                resourceId,
                null,
                false,
                errorMessage
        );
        auditLogRepository.save(log);
        
        // 审计日志：结构化输出到独立文件
        auditLogger.warn("ACTION={} | RESOURCE_TYPE={} | RESOURCE_ID={} | USER={} | USER_ID={} | RESULT=FAILED | ERROR={}",
                action, resourceType, resourceId != null ? resourceId : "N/A", username, userId, errorMessage);
    }
}

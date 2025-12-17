package com.study.service;

import com.study.config.PermissionCodes;
import com.study.context.SessionContext;
import com.study.domain.AuditLog;
import com.study.repository.AuditLogRepository;

import java.util.List;

/**
 * Audit log query service
 */
public class AuditService extends BaseService {
    private final AuditLogRepository auditLogRepository;
    
    public AuditService(SessionContext sessionContext,
                       AuditLogRepository auditLogRepository) {
        super(sessionContext, auditLogRepository);
        this.auditLogRepository = auditLogRepository;
    }
    
    /**
     * View audit logs for current user
     */
    public List<AuditLog> viewMyAuditLogs(int limit) {
        return executeWithTemplate(
            PermissionCodes.AUDIT_VIEW,
            "VIEW_MY_AUDIT",
            "AuditLog",
            null,
            () -> {
                if (!sessionContext.isLoggedIn()) {
                    throw new IllegalStateException("User not logged in");
                }
            },
            () -> {
                Long userId = sessionContext.getCurrentUser().getId();
                return auditLogRepository.findByUserId(userId, limit);
            }
        );
    }
    
    /**
     * View audit logs for specific user
     */
    public List<AuditLog> viewUserAuditLogs(Long userId, int limit) {
        return executeWithTemplate(
            PermissionCodes.AUDIT_VIEW_ALL,
            "VIEW_USER_AUDIT",
            "AuditLog",
            String.valueOf(userId),
            () -> validateNotNull(userId, "User ID"),
            () -> auditLogRepository.findByUserId(userId, limit)
        );
    }
    
    /**
     * View all audit logs (admin function)
     */
    public List<AuditLog> viewAllAuditLogs(int limit) {
        return executeWithTemplate(
            PermissionCodes.AUDIT_VIEW_ALL,
            "VIEW_ALL_AUDIT",
            "AuditLog",
            null,
            null,
            () -> auditLogRepository.findAll(limit)
        );
    }
    
    /**
     * View audit logs by action
     */
    public List<AuditLog> viewAuditLogsByAction(String action, int limit) {
        return executeWithTemplate(
            PermissionCodes.AUDIT_VIEW_ALL,
            "VIEW_AUDIT_BY_ACTION",
            "AuditLog",
            action,
            () -> validateNotBlank(action, "Action"),
            () -> auditLogRepository.findByAction(action, limit)
        );
    }

    /**
     * View audit logs by resource.
     */
    public List<AuditLog> viewAuditLogsByResource(String resourceType, String resourceId, int limit) {
        return executeWithTemplate(
                PermissionCodes.AUDIT_VIEW_ALL,
                "VIEW_AUDIT_BY_RESOURCE",
                resourceType,
                resourceId,
                () -> validateNotBlank(resourceType, "Resource type"),
                () -> auditLogRepository.findByResource(resourceType, resourceId, limit)
        );
    }
}

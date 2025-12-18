package com.study.service;

import com.study.config.PermissionCodes;
import com.study.context.SessionContext;
import com.study.domain.Permission;
import com.study.domain.ScopedPermission;
import com.study.exception.ValidationException;
import com.study.repository.AuditLogRepository;
import com.study.repository.PermissionRepository;

import java.util.List;

/**
 * Permission management service
 */
public class PermissionService extends BaseService {
    private final PermissionRepository permissionRepository;
    
    public PermissionService(SessionContext sessionContext,
                            PermissionRepository permissionRepository,
                            AuditLogRepository auditLogRepository) {
        super(sessionContext, auditLogRepository);
        this.permissionRepository = permissionRepository;
    }
    
    /**
     * Create new permission
     */
    public Permission createPermission(String code, String name, String description) {
        return executeWithTemplate(
            PermissionCodes.PERMISSION_CREATE,
            "CREATE_PERMISSION",
            "Permission",
            code,
            () -> {
                validateNotBlank(code, "Permission code");
                validateNotBlank(name, "Permission name");
                if (code.length() < 2 || code.length() > 50) {
                    throw new ValidationException("Permission code must be between 2 and 50 characters");
                }
            },
            () -> {
                Permission permission = new Permission();
                permission.setCode(code);
                permission.setName(name);
                permission.setDescription(description);
                
                Permission savedPermission = permissionRepository.save(permission);
                logger.info("Permission created: {}", code);
                return savedPermission;
            }
        );
    }

    /**
     * Update permission meta info.
     */
    public Permission updatePermission(String code, String name, String description, Long resourceId) {
        return executeWithTemplate(
                PermissionCodes.PERMISSION_UPDATE,
                "UPDATE_PERMISSION",
                "Permission",
                code,
                () -> validateNotBlank(code, "Permission code"),
                () -> {
                    Permission permission = permissionRepository.findByCode(code)
                            .orElseThrow(() -> new ValidationException("Permission not found: " + code));
                    if (name != null && !name.isBlank()) {
                        permission.setName(name);
                    }
                    if (description != null && !description.isBlank()) {
                        permission.setDescription(description);
                    }
                    permission.setResourceId(resourceId);
                    permissionRepository.update(permission);
                    return permission;
                }
        );
    }

    /**
     * Delete a permission by code.
     */
    public void deletePermission(String code) {
        executeWithTemplate(
                PermissionCodes.PERMISSION_DELETE,
                "DELETE_PERMISSION",
                "Permission",
                code,
                () -> validateNotBlank(code, "Permission code"),
                () -> {
                    Permission permission = permissionRepository.findByCode(code)
                            .orElseThrow(() -> new ValidationException("Permission not found: " + code));
                    permissionRepository.delete(permission.getId());
                    logger.info("Permission deleted: {}", code);
                }
        );
    }
    
    /**
     * List all permissions
     */
    public List<Permission> listPermissions() {
        return executeWithTemplate(
            PermissionCodes.PERMISSION_VIEW,
            "LIST_PERMISSIONS",
            "Permission",
            null,
            null,
            permissionRepository::findAll
        );
    }
    
    /**
     * List my permissions (current logged-in user)
     */
    public List<Permission> listMyPermissions() {
        return executeWithTemplate(
            null, // No permission check needed - users can always view their own permissions
            "VIEW_MY_PERMISSIONS",
            "UserPermission",
            null,
            null,
            () -> {
                Long currentUserId = sessionContext.getCurrentUser().getId();
                return permissionRepository.findByUserId(currentUserId);
            }
        );
    }
    
    /**
     * Get permission by code
     */
    public Permission getPermissionByCode(String code) {
        return executeWithTemplate(
            PermissionCodes.PERMISSION_VIEW,
            "VIEW_PERMISSION",
            "Permission",
            code,
            () -> validateNotBlank(code, "Permission code"),
            () -> permissionRepository.findByCode(code)
                .orElseThrow(() -> new ValidationException("Permission not found: " + code))
        );
    }
    
    /**
     * Assign permission to role
     */
    public void assignPermissionToRole(Long roleId, Long permissionId) {
        executeWithTemplate(
            PermissionCodes.PERMISSION_ASSIGN,
            "ASSIGN_PERMISSION",
            "RolePermission",
            roleId + "-" + permissionId,
            () -> {
                validateNotNull(roleId, "Role ID");
                validateNotNull(permissionId, "Permission ID");
            },
            () -> {
                permissionRepository.assignPermissionToRole(roleId, permissionId);
                logger.info("Permission {} assigned to role {}", permissionId, roleId);
            }
        );
    }
    
    /**
     * Remove permission from role
     */
    public void removePermissionFromRole(Long roleId, Long permissionId) {
        executeWithTemplate(
            PermissionCodes.PERMISSION_ASSIGN,
            "REMOVE_PERMISSION",
            "RolePermission",
            roleId + "-" + permissionId,
            () -> {
                validateNotNull(roleId, "Role ID");
                validateNotNull(permissionId, "Permission ID");
            },
            () -> {
                permissionRepository.removePermissionFromRole(roleId, permissionId);
                logger.info("Permission {} removed from role {}", permissionId, roleId);
            }
        );
    }
    
    /**
     * Get permissions by role ID
     */
    public List<Permission> getPermissionsByRoleId(Long roleId) {
        return executeWithTemplate(
            PermissionCodes.PERMISSION_VIEW,
            "VIEW_ROLE_PERMISSIONS",
            "RolePermission",
            String.valueOf(roleId),
            () -> validateNotNull(roleId, "Role ID"),
            () -> permissionRepository.findByRoleId(roleId)
        );
    }
    
    /**
     * Get permissions by user ID
     */
    public List<Permission> getPermissionsByUserId(Long userId) {
        return executeWithTemplate(
            PermissionCodes.PERMISSION_VIEW,
            "VIEW_USER_PERMISSIONS",
            "UserPermission",
            String.valueOf(userId),
            () -> validateNotNull(userId, "User ID"),
            () -> permissionRepository.findByUserId(userId)
        );
    }

    /**
     * Assign a scoped permission to a role.
     */
    public void assignScopedPermissionToRole(Long roleId, String permissionCode, String resourceType, String resourceId) {
        executeWithTemplate(
                PermissionCodes.RESOURCE_GRANT,
                "ASSIGN_SCOPED_PERMISSION",
                resourceType,
                resourceId,
                () -> {
                    validateNotNull(roleId, "Role ID");
                    validateNotBlank(permissionCode, "Permission code");
                    validateNotBlank(resourceType, "Resource type");
                },
                () -> {
                    String normalizedType = resourceType.trim();
                    List<ScopedPermission> existing = permissionRepository.findScopedPermissionsByRoleId(roleId);
                    boolean hasGlobal = existing.stream()
                            .anyMatch(s -> permissionCode.equals(s.getPermissionCode())
                                    && normalizedType.equalsIgnoreCase(s.getResourceType())
                                    && (s.getResourceId() == null || s.getResourceId().isBlank()));
                    boolean hasSpecific = existing.stream()
                            .anyMatch(s -> permissionCode.equals(s.getPermissionCode())
                                    && normalizedType.equalsIgnoreCase(s.getResourceType())
                                    && s.getResourceId() != null && !s.getResourceId().isBlank());

                    boolean incomingGlobal = resourceId == null || resourceId.isBlank();
                    if (incomingGlobal) {
                        if (hasGlobal) {
                            throw new ValidationException("Already has global scope for this permission/resourceType");
                        }
                        if (hasSpecific) {
                            permissionRepository.clearScopedPermissions(roleId, permissionCode, normalizedType);
                        }
                    } else if (hasGlobal) {
                        throw new ValidationException("Global scope already exists; remove it before adding specific scope");
                    }

                    permissionRepository.assignScopedPermission(roleId, permissionCode, normalizedType, resourceId);
                }
        );
    }

    /**
     * Remove a scoped permission from a role.
     */
    public void removeScopedPermissionFromRole(Long roleId, String permissionCode, String resourceType, String resourceId) {
        executeWithTemplate(
                PermissionCodes.RESOURCE_GRANT,
                "REMOVE_SCOPED_PERMISSION",
                resourceType,
                resourceId,
                () -> {
                    validateNotNull(roleId, "Role ID");
                    validateNotBlank(permissionCode, "Permission code");
                    validateNotBlank(resourceType, "Resource type");
                },
                () -> permissionRepository.removeScopedPermission(roleId, permissionCode, resourceType, resourceId)
        );
    }

    public List<ScopedPermission> getScopedPermissionsByRole(Long roleId) {
        return executeWithTemplate(
                PermissionCodes.PERMISSION_VIEW,
                "VIEW_SCOPED_PERMISSIONS",
                "RolePermissionScope",
                roleId != null ? roleId.toString() : null,
                () -> validateNotNull(roleId, "Role ID"),
                () -> permissionRepository.findScopedPermissionsByRoleId(roleId)
        );
    }
}

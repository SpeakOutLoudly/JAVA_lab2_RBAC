package com.study.service;

import com.study.config.PermissionCodes;
import com.study.context.SessionContext;
import com.study.domain.Permission;
import com.study.domain.Role;
import com.study.exception.ValidationException;
import com.study.repository.AuditLogRepository;
import com.study.repository.PermissionRepository;
import com.study.repository.RoleRepository;

import java.util.List;

/**
 * Role management service
 */
public class RoleService extends BaseService {
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    
    public RoleService(SessionContext sessionContext,
                      RoleRepository roleRepository,
                      PermissionRepository permissionRepository,
                      AuditLogRepository auditLogRepository) {
        super(sessionContext, auditLogRepository);
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
    }
    
    /**
     * Create new role
     */
    public Role createRole(String code, String name, String description) {
        return executeWithTemplate(
            PermissionCodes.ROLE_CREATE,
            "CREATE_ROLE",
            "Role",
            code,
            () -> {
                validateNotBlank(code, "Role code");
                validateNotBlank(name, "Role name");
                if (code.length() < 2 || code.length() > 50) {
                    throw new ValidationException("Role code must be between 2 and 50 characters");
                }
            },
            () -> {
                Role role = new Role();
                role.setCode(code);
                role.setName(name);
                role.setDescription(description);
                
                Role savedRole = roleRepository.save(role);
                logger.info("Role created: {}", code);
                return savedRole;
            }
        );
    }

    /**
     * Update role metadata.
     */
    public Role updateRole(Long roleId, String name, String description) {
        return executeWithTemplate(
                PermissionCodes.ROLE_UPDATE,
                "UPDATE_ROLE",
                "Role",
                roleId != null ? roleId.toString() : null,
                () -> validateNotNull(roleId, "Role ID"),
                () -> {
                    Role role = roleRepository.findById(roleId)
                            .orElseThrow(() -> new ValidationException("Role not found: " + roleId));
                    if (name != null && !name.isBlank()) {
                        role.setName(name);
                    }
                    role.setDescription(description);
                    roleRepository.update(role);
                    return role;
                }
        );
    }

    /**
     * Delete a role.
     */
    public void deleteRole(Long roleId) {
        executeWithTemplate(
                PermissionCodes.ROLE_DELETE,
                "DELETE_ROLE",
                "Role",
                roleId != null ? roleId.toString() : null,
                () -> validateNotNull(roleId, "Role ID"),
                () -> roleRepository.delete(roleId)
        );
    }
    
    /**
     * List all roles
     */
    public List<Role> listRoles() {
        return executeWithTemplate(
            PermissionCodes.ROLE_VIEW,
            "LIST_ROLES",
            "Role",
            null,
            null,
            roleRepository::findAll
        );
    }
    
    /**
     * Get role by code
     */
    public Role getRoleByCode(String code) {
        return executeWithTemplate(
            PermissionCodes.ROLE_VIEW,
            "VIEW_ROLE",
            "Role",
            code,
            () -> validateNotBlank(code, "Role code"),
            () -> roleRepository.findByCode(code)
                .orElseThrow(() -> new ValidationException("Role not found: " + code))
        );
    }
    
    /**
     * Assign role to user
     */
    public void assignRoleToUser(Long userId, Long roleId) {
        executeWithTemplate(
            PermissionCodes.ROLE_ASSIGN,
            "ASSIGN_ROLE",
            "UserRole",
            userId + "-" + roleId,
            () -> {
                validateNotNull(userId, "User ID");
                validateNotNull(roleId, "Role ID");
            },
            () -> {
                roleRepository.assignRoleToUser(userId, roleId);
                logger.info("Role {} assigned to user {}", roleId, userId);
            }
        );
    }
    
    /**
     * Remove role from user
     */
    public void removeRoleFromUser(Long userId, Long roleId) {
        executeWithTemplate(
            PermissionCodes.ROLE_ASSIGN,
            "REMOVE_ROLE",
            "UserRole",
            userId + "-" + roleId,
            () -> {
                validateNotNull(userId, "User ID");
                validateNotNull(roleId, "Role ID");
            },
            () -> {
                roleRepository.removeRoleFromUser(userId, roleId);
                logger.info("Role {} removed from user {}", roleId, userId);
            }
        );
    }
    
    /**
     * Get roles by user ID
     */
    public List<Role> getRolesByUserId(Long userId) {
        return executeWithTemplate(
            PermissionCodes.ROLE_VIEW,
            "VIEW_USER_ROLES",
            "UserRole",
            String.valueOf(userId),
            () -> validateNotNull(userId, "User ID"),
            () -> roleRepository.findByUserId(userId)
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
}

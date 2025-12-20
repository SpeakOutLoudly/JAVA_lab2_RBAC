package com.study.facade;

import com.study.config.CommandSpec;
import com.study.context.SessionContext;
import com.study.domain.AuditLog;
import com.study.domain.Permission;
import com.study.domain.Resource;
import com.study.domain.Role;
import com.study.domain.ScopedPermission;
import com.study.domain.User;
import com.study.service.dto.ResourceAccessView;
import com.study.exception.ValidationException;
import com.study.repository.*;
import com.study.service.*;

import java.util.List;

/**
 * Facade to simplify CLI interaction with services
 * Provides coarse-grained use case methods
 */
public class RbacFacade {
    private final SessionContext sessionContext;
    private final AuthService authService;
    private final UserService userService;
    private final RoleService roleService;
    private final PermissionService permissionService;
    private final AuditService auditService;
    private final ResourceService resourceService;
    private final AuditLogRepository auditLogRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    
    public RbacFacade(DatabaseConnection dbConnection) {
        // Initialize repositories
        UserRepository userRepository = new UserRepository(dbConnection);
        RoleRepository roleRepository = new RoleRepository(dbConnection);
        PermissionRepository permissionRepository = new PermissionRepository(dbConnection);
        ResourceRepository resourceRepository = new ResourceRepository(dbConnection);
        AuditLogRepository auditLogRepository = new AuditLogRepository(dbConnection);
        
        // Initialize session context
        this.sessionContext = new SessionContext();
        
        // Initialize services
        this.authService = new AuthService(sessionContext, userRepository, 
                                          permissionRepository, auditLogRepository);
        this.userService = new UserService(sessionContext, userRepository, 
                                          roleRepository, auditLogRepository);
        this.roleService = new RoleService(sessionContext, roleRepository, 
                                          permissionRepository, auditLogRepository);
        this.permissionService = new PermissionService(sessionContext, 
                                          permissionRepository, auditLogRepository);
        this.resourceService = new ResourceService(sessionContext, resourceRepository, 
                                          permissionRepository, auditLogRepository);
        this.auditService = new AuditService(sessionContext, auditLogRepository);
        
        this.auditLogRepository = auditLogRepository;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        
    }
    
    // Auth operations
    public User login(String username, String password) {
        return authService.login(username, password);
    }
    
    public void logout() {
        authService.logout();
    }
    
    public void changePassword(String oldPassword, String newPassword) {
        authService.changePassword(oldPassword, newPassword);
    }
    
    public boolean isLoggedIn() {
        return sessionContext.isLoggedIn();
    }
    
    public User getCurrentUser() {
        return sessionContext.getCurrentUser();
    }
    
    // User operations
    public User createUser(String username, String password) {
        return userService.createUser(username, password, null);
    }
    
    public User createUser(String username, String password, String email, String phone, String realName) {
        return userService.createUser(username, password, null, email, phone, realName);
    }
    
    public User createUserWithRole(String username, String password, String roleCode) {
        Role role = roleRepository.findByCode(roleCode)
            .orElseThrow(() -> new ValidationException("Role not found: " + roleCode));
        return userService.createUser(username, password, role.getId());
    }
    
    public User createUserWithRole(String username, String password, String roleCode,
                                   String email, String phone, String realName) {
        Role role = roleRepository.findByCode(roleCode)
            .orElseThrow(() -> new ValidationException("Role not found: " + roleCode));
        return userService.createUser(username, password, role.getId(), email, phone, realName);
    }
    
    public List<User> listUsers() {
        return userService.listUsers();
    }
    
    public User viewUser(String username) {
        return userService.getUserByUsername(username);
    }
    
    public void deleteUser(Long userId) {
        userService.deleteUser(userId);
    }

    public void resetPassword(Long userId, String newPassword) {
        userService.resetPassword(userId, newPassword);
    }
    
    public void enableUser(Long userId) {
        userService.setUserEnabled(userId, true);
    }
    
    public void disableUser(Long userId) {
        userService.setUserEnabled(userId, false);
    }
    
    public void updateUserInfo(String email, String phone, String realName) {
        Long userId = sessionContext.getCurrentUser().getId();
        userService.updateUserInfo(userId, email, phone, realName);
    }
    
    // Role operations
    public Role createRole(String code, String name, String description) {
        return roleService.createRole(code, name, description);
    }
    
    public List<Role> listRoles() {
        return roleService.listRoles();
    }
    
    public Role getRoleByCode(String code) {
        return roleService.getRoleByCode(code);
    }

    public Role updateRole(Long roleId, String name, String description) {
        return roleService.updateRole(roleId, name, description);
    }

    public void deleteRole(Long roleId) {
        roleService.deleteRole(roleId);
    }
    
    public void assignRoleToUser(String username, String roleCode) {
        User user = userService.getUserByUsername(username);
        Role role = roleService.getRoleByCode(roleCode);
        roleService.assignRoleToUser(user.getId(), role.getId());
        
        // Refresh permissions if current user
        if (sessionContext.isLoggedIn() && 
            sessionContext.getCurrentUser().getId().equals(user.getId())) {
            authService.refreshCurrentUserPermissions();
        }
    }
    
    public void removeRoleFromUser(String username, String roleCode) {
        User user = userService.getUserByUsername(username);
        Role role = roleService.getRoleByCode(roleCode);
        roleService.removeRoleFromUser(user.getId(), role.getId());
        
        // Refresh permissions if current user
        if (sessionContext.isLoggedIn() && 
            sessionContext.getCurrentUser().getId().equals(user.getId())) {
            authService.refreshCurrentUserPermissions();
        }
    }
    
    public List<Role> getUserRoles(String username) {
        User user = userService.getUserByUsername(username);
        return roleService.getRolesByUserId(user.getId());
    }
    
    public List<Permission> getRolePermissions(String roleCode) {
        Role role = roleService.getRoleByCode(roleCode);
        return roleService.getPermissionsByRoleId(role.getId());
    }
    
    // Permission operations
    public Permission createPermission(String code, String name, String description) {
        return permissionService.createPermission(code, name, description);
    }
    
    public List<Permission> listPermissions() {
        return permissionService.listPermissions();
    }
    
    public List<Permission> listMyPermissions() {
        return permissionService.listMyPermissions();
    }
    
    public Permission getPermissionByCode(String code) {
        return permissionService.getPermissionByCode(code);
    }
    
    public void assignPermissionToRole(String roleCode, String permissionCode) {
        Role role = roleService.getRoleByCode(roleCode);
        Permission permission = permissionService.getPermissionByCode(permissionCode);
        permissionService.assignPermissionToRole(role.getId(), permission.getId());
    }
    
    public void removePermissionFromRole(String roleCode, String permissionCode) {
        Role role = roleService.getRoleByCode(roleCode);
        Permission permission = permissionService.getPermissionByCode(permissionCode);
        permissionService.removePermissionFromRole(role.getId(), permission.getId());
    }
    
    public List<Permission> getUserPermissions(String username) {
        User user = userService.getUserByUsername(username);
        return permissionService.getPermissionsByUserId(user.getId());
    }

    public Permission updatePermission(String code, String name, String description, Long resourceId) {
        return permissionService.updatePermission(code, name, description, resourceId);
    }

    public void deletePermission(String code) {
        permissionService.deletePermission(code);
    }

    public void assignScopedPermission(String roleCode, String permissionCode, String resourceType, String resourceId) {
        Role role = roleService.getRoleByCode(roleCode);
        permissionService.assignScopedPermissionToRole(role.getId(), permissionCode, resourceType, resourceId);

        if (sessionContext.isLoggedIn()) {
            User current = sessionContext.getCurrentUser();
            java.util.List<Role> userRoles = roleRepository.findByUserId(current.getId());
            boolean currentHasRole = userRoles.stream()
                    .anyMatch(r -> r.getId().equals(role.getId()));
            if (currentHasRole) {
                authService.refreshCurrentUserPermissions();
            }
        }
    }

    public void removeScopedPermission(String roleCode, String permissionCode, String resourceType, String resourceId) {
        Role role = roleService.getRoleByCode(roleCode);
        permissionService.removeScopedPermissionFromRole(role.getId(), permissionCode, resourceType, resourceId);

        if (sessionContext.isLoggedIn()) {
            User current = sessionContext.getCurrentUser();
            java.util.List<Role> userRoles = roleRepository.findByUserId(current.getId());
            boolean currentHasRole = userRoles.stream()
                    .anyMatch(r -> r.getId().equals(role.getId()));
            if (currentHasRole) {
                authService.refreshCurrentUserPermissions();
            }
        }
    }

    public List<ScopedPermission> getScopedPermissionsForRole(String roleCode) {
        Role role = roleService.getRoleByCode(roleCode);
        return permissionService.getScopedPermissionsByRole(role.getId());
    }

    // Resource operations
    public Resource createResource(String code, String name, String type, String url) {
        return resourceService.createResource(code, name, type, url);
    }

    public Resource updateResource(Long resourceId, String name, String type, String url) {
        return resourceService.updateResource(resourceId, name, type, url);
    }

    public void deleteResource(Long resourceId) {
        resourceService.deleteResource(resourceId);
    }

    public List<Resource> listResources() {
        return resourceService.listResources();
    }

    public List<Resource> listMyResources() {
        return resourceService.listMyResources();
    }

    public Resource getResource(Long resourceId) {
        return resourceService.getResource(resourceId);
    }

    public ResourceAccessView getResourceAccess(Long resourceId) {
        return resourceService.getResourceAccess(resourceId);
    }
    
    // Audit operations
    public List<AuditLog> viewMyAuditLogs(int limit) {
        return auditService.viewMyAuditLogs(limit);
    }
    
    public List<AuditLog> viewUserAuditLogs(Long userId, int limit) {
        return auditService.viewUserAuditLogs(userId, limit);
    }
    
    public List<AuditLog> viewAllAuditLogs(int limit) {
        return auditService.viewAllAuditLogs(limit);
    }

    public List<AuditLog> viewAuditLogsByAction(String action, int limit) {
        return auditService.viewAuditLogsByAction(action, limit);
    }

    public List<AuditLog> viewAuditLogsByResource(String resourceType, String resourceId, int limit) {
        return auditService.viewAuditLogsByResource(resourceType, resourceId, limit);
    }
    
    // Command permission check
    public boolean canExecuteCommand(String commandName) {
        CommandSpec spec = CommandSpec.fromCommand(commandName);
        if (spec == null || !spec.requiresPermission()) {
            return true;
        }
        return authService.hasPermission(spec.getRequiredPermission());
    }
    
    // Get available commands for current user
    public List<CommandSpec> getAvailableCommands() {
        return java.util.Arrays.stream(CommandSpec.values())
            .filter(spec -> !spec.requiresPermission() || 
                          authService.hasPermission(spec.getRequiredPermission()))
            .toList();
    }
}

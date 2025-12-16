package com.study.facade;

import com.study.config.CommandSpec;
import com.study.context.SessionContext;
import com.study.domain.Permission;
import com.study.domain.Role;
import com.study.domain.User;
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
    private final AuditLogRepository auditLogRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    
    public RbacFacade(DatabaseConnection dbConnection) {
        // Initialize repositories
        UserRepository userRepository = new UserRepository(dbConnection);
        RoleRepository roleRepository = new RoleRepository(dbConnection);
        PermissionRepository permissionRepository = new PermissionRepository(dbConnection);
        AuditLogRepository auditLogRepository = new AuditLogRepository(dbConnection);
        
        // Initialize session context
        this.sessionContext = new SessionContext();
        
        // Initialize services
        this.authService = new AuthService(sessionContext, userRepository, 
                                          permissionRepository, auditLogRepository);
        this.userService = new UserService(sessionContext, userRepository, 
                                          roleRepository, auditLogRepository);
        this.auditLogRepository = auditLogRepository;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        
        // Bootstrap system on first run
        BootstrapService bootstrapService = new BootstrapService(
            userRepository, roleRepository, permissionRepository);
        bootstrapService.initializeSystem();
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
    
    public User createUserWithRole(String username, String password, String roleCode) {
        Role role = roleRepository.findByCode(roleCode)
            .orElseThrow(() -> new RuntimeException("Role not found: " + roleCode));
        return userService.createUser(username, password, role.getId());
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
    
    // Role operations
    public List<Role> listRoles() {
        return roleRepository.findAll();
    }
    
    public void assignRoleToUser(String username, String roleCode) {
        User user = userService.getUserByUsername(username);
        Role role = roleRepository.findByCode(roleCode)
            .orElseThrow(() -> new RuntimeException("Role not found: " + roleCode));
        roleRepository.assignRoleToUser(user.getId(), role.getId());
        
        // Refresh permissions if current user
        if (sessionContext.isLoggedIn() && 
            sessionContext.getCurrentUser().getId().equals(user.getId())) {
            authService.refreshCurrentUserPermissions();
        }
    }
    
    public List<Role> getUserRoles(String username) {
        User user = userService.getUserByUsername(username);
        return roleRepository.findByUserId(user.getId());
    }
    
    // Permission operations
    public List<Permission> listPermissions() {
        return permissionRepository.findAll();
    }
    
    public List<Permission> getUserPermissions(String username) {
        User user = userService.getUserByUsername(username);
        return permissionRepository.findByUserId(user.getId());
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

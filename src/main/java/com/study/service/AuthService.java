package com.study.service;

import com.study.config.PermissionCodes;
import com.study.context.SessionContext;
import com.study.domain.Permission;
import com.study.domain.User;
import com.study.exception.PermissionDeniedException;
import com.study.exception.ValidationException;
import com.study.repository.AuditLogRepository;
import com.study.repository.PermissionRepository;
import com.study.repository.UserRepository;
import com.study.security.PasswordEncoder;
import com.study.security.Sha256PasswordEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Authentication and Authorization Service
 */
public class AuthService extends BaseService {
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    
    private final UserRepository userRepository;
    private final PermissionRepository permissionRepository;
    private final PasswordEncoder passwordEncoder;
    
    public AuthService(SessionContext sessionContext,
                      UserRepository userRepository,
                      PermissionRepository permissionRepository,
                      AuditLogRepository auditLogRepository) {
        super(sessionContext, auditLogRepository);
        this.userRepository = userRepository;
        this.permissionRepository = permissionRepository;
        this.passwordEncoder = new Sha256PasswordEncoder();
    }
    
    /**
     * User login - loads user and caches permissions
     */
    public User login(String username, String password) {
        return executeWithTemplate(
            null, // No permission required for login
            "LOGIN",
            "User",
            username,
            () -> {
                validateNotBlank(username, "Username");
                validateNotBlank(password, "Password");
            },
            () -> {
                User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new ValidationException("User not found: " + username));
                
                if (!user.isEnabled()) {
                    throw new ValidationException("User account is disabled");
                }
                
                if (!passwordEncoder.matches(password, user.getPasswordHash(), user.getSalt())) {
                    throw new ValidationException("Invalid password");
                }
                
                // Load and cache permissions
                List<Permission> permissions = permissionRepository.findByUserId(user.getId());
                var scopedPermissions = permissionRepository.findScopedPermissionsByUserId(user.getId());
                sessionContext.setCurrentUser(user);
                sessionContext.setPermissions(permissions, scopedPermissions);
                
                logger.info("User logged in: {}, permissions loaded: {}", 
                        username, permissions.size());
                logger.info("login scopedPermissions size={}", sessionContext.getScopedPermissions().size());
                logger.info("login scopedPermissions={}", sessionContext.getScopedPermissions());

                return user;
            }
        );
    }
    
    /**
     * Logout current user
     */
    public void logout() {
        if (sessionContext.isLoggedIn()) {
            String username = sessionContext.getCurrentUser().getUsername();
            auditSuccess("LOGOUT", "User", username, null);
            logger.info("User logged out: {}", username);
            sessionContext.clear();
        }
    }
    
    /**
     * Change password for current user
     */
    public void changePassword(String oldPassword, String newPassword) {
        executeWithTemplate(
            null, // User changing own password doesn't need specific permission
            "CHANGE_PASSWORD",
            "User",
            sessionContext.getCurrentUser().getUsername(),
            () -> {
                if (!sessionContext.isLoggedIn()) {
                    throw new PermissionDeniedException("Not logged in");
                }
                validateNotBlank(oldPassword, "Old password");
                validateNotBlank(newPassword, "New password");
                if (newPassword.length() < 6) {
                    throw new ValidationException("Password must be at least 6 characters");
                }
            },
            () -> {
                User user = sessionContext.getCurrentUser();
                
                // Verify old password
                if (!passwordEncoder.matches(oldPassword, user.getPasswordHash(), user.getSalt())) {
                    throw new ValidationException("Old password is incorrect");
                }
                
                // Update to new password
                String newSalt = passwordEncoder.generateSalt();
                String newHash = passwordEncoder.encode(newPassword, newSalt);
                user.setPasswordHash(newHash);
                user.setSalt(newSalt);
                
                userRepository.update(user);
                logger.info("Password changed for user: {}", user.getUsername());
            }
        );
    }
    
    /**
     * Refresh permissions for current user (called after role/permission changes)
     */
    public void refreshCurrentUserPermissions() {
        if (sessionContext.isLoggedIn()) {
            Long userId = sessionContext.getCurrentUser().getId();
            List<Permission> permissions = permissionRepository.findByUserId(userId);
            var scopedPermissions = permissionRepository.findScopedPermissionsByUserId(userId);
            sessionContext.refreshPermissions(permissions, scopedPermissions);
            logger.info("Permissions refreshed for user: {}, count: {}",
                    sessionContext.getCurrentUser().getUsername(), permissions.size());
        }
    }
    
    /**
     * Check if current user has specific permission
     */
    public boolean hasPermission(String permissionCode) {
        return sessionContext.isLoggedIn() && 
               sessionContext.hasPermission(permissionCode);
    }
}

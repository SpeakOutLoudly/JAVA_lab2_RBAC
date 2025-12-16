package com.study.service;

import com.study.config.PermissionCodes;
import com.study.context.SessionContext;
import com.study.domain.User;
import com.study.exception.DataNotFoundException;
import com.study.exception.ValidationException;
import com.study.repository.AuditLogRepository;
import com.study.repository.RoleRepository;
import com.study.repository.UserRepository;
import com.study.security.PasswordEncoder;
import com.study.security.Sha256PasswordEncoder;

import java.util.List;

/**
 * User management service
 */
public class UserService extends BaseService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    
    public UserService(SessionContext sessionContext,
                      UserRepository userRepository,
                      RoleRepository roleRepository,
                      AuditLogRepository auditLogRepository) {
        super(sessionContext, auditLogRepository);
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = new Sha256PasswordEncoder();
    }
    
    /**
     * Create new user with optional default role assignment
     */
    public User createUser(String username, String password, Long defaultRoleId) {
        return executeWithTemplate(
            PermissionCodes.USER_CREATE,
            "CREATE_USER",
            "User",
            username,
            () -> {
                validateNotBlank(username, "Username");
                validateNotBlank(password, "Password");
                if (password.length() < 6) {
                    throw new ValidationException("Password must be at least 6 characters");
                }
                if (username.length() < 3 || username.length() > 50) {
                    throw new ValidationException("Username must be between 3 and 50 characters");
                }
            },
            () -> {
                // Create user with hashed password
                String salt = passwordEncoder.generateSalt();
                String passwordHash = passwordEncoder.encode(password, salt);
                
                User user = new User();
                user.setUsername(username);
                user.setPasswordHash(passwordHash);
                user.setSalt(salt);
                user.setEnabled(true);
                
                User savedUser = userRepository.save(user);
                
                // Assign default role if specified
                if (defaultRoleId != null) {
                    roleRepository.assignRoleToUser(savedUser.getId(), defaultRoleId);
                }
                
                logger.info("User created: {}", username);
                return savedUser;
            }
        );
    }
    
    /**
     * List all users
     */
    public List<User> listUsers() {
        return executeWithTemplate(
            PermissionCodes.USER_LIST,
            "LIST_USERS",
            "User",
            null,
            null,
            userRepository::findAll
        );
    }
    
    /**
     * Get user by ID
     */
    public User getUserById(Long userId) {
        return executeWithTemplate(
            PermissionCodes.USER_VIEW,
            "VIEW_USER",
            "User",
            String.valueOf(userId),
            () -> validateNotNull(userId, "User ID"),
            () -> userRepository.findById(userId)
                .orElseThrow(() -> new DataNotFoundException("User not found: " + userId))
        );
    }
    
    /**
     * Get user by username
     */
    public User getUserByUsername(String username) {
        return executeWithTemplate(
            PermissionCodes.USER_VIEW,
            "VIEW_USER",
            "User",
            username,
            () -> validateNotBlank(username, "Username"),
            () -> userRepository.findByUsername(username)
                .orElseThrow(() -> new DataNotFoundException("User not found: " + username))
        );
    }
    
    /**
     * Enable/Disable user
     */
    public void setUserEnabled(Long userId, boolean enabled) {
        executeWithTemplate(
            PermissionCodes.USER_UPDATE,
            enabled ? "ENABLE_USER" : "DISABLE_USER",
            "User",
            String.valueOf(userId),
            () -> validateNotNull(userId, "User ID"),
            () -> {
                User user = userRepository.findById(userId)
                    .orElseThrow(() -> new DataNotFoundException("User not found: " + userId));
                
                user.setEnabled(enabled);
                userRepository.update(user);
                
                logger.info("User {} {}", user.getUsername(), enabled ? "enabled" : "disabled");
            }
        );
    }
    
    /**
     * Delete user
     */
    public void deleteUser(Long userId) {
        executeWithTemplate(
            PermissionCodes.USER_DELETE,
            "DELETE_USER",
            "User",
            String.valueOf(userId),
            () -> validateNotNull(userId, "User ID"),
            () -> {
                User user = userRepository.findById(userId)
                    .orElseThrow(() -> new DataNotFoundException("User not found: " + userId));
                
                userRepository.delete(userId);
                logger.info("User deleted: {}", user.getUsername());
            }
        );
    }
    
    /**
     * Reset user password (admin function)
     */
    public void resetPassword(Long userId, String newPassword) {
        executeWithTemplate(
            PermissionCodes.USER_UPDATE,
            "RESET_PASSWORD",
            "User",
            String.valueOf(userId),
            () -> {
                validateNotNull(userId, "User ID");
                validateNotBlank(newPassword, "New password");
                if (newPassword.length() < 6) {
                    throw new ValidationException("Password must be at least 6 characters");
                }
            },
            () -> {
                User user = userRepository.findById(userId)
                    .orElseThrow(() -> new DataNotFoundException("User not found: " + userId));
                
                String newSalt = passwordEncoder.generateSalt();
                String newHash = passwordEncoder.encode(newPassword, newSalt);
                user.setPasswordHash(newHash);
                user.setSalt(newSalt);
                
                userRepository.update(user);
                logger.info("Password reset for user: {}", user.getUsername());
            }
        );
    }
}

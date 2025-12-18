package com.study.service;

import com.study.config.PermissionCodes;
import com.study.context.SessionContext;
import com.study.domain.User;
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
        return createUser(username, password, defaultRoleId, null, null, null);
    }
    
    /**
     * Create new user with optional default role assignment and user info
     */
    public User createUser(String username, String password, Long defaultRoleId, 
                          String email, String phone, String realName) {
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
                // Email validation
                if (email != null && !email.isBlank() && !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                    throw new ValidationException("Invalid email format");
                }
                // Phone validation
                if (phone != null && !phone.isBlank() && !phone.matches("^[0-9+\\-() ]{6,20}$")) {
                    throw new ValidationException("Invalid phone format");
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
                user.setEmail(email);
                user.setPhone(phone);
                user.setRealName(realName);
                
                User savedUser = userRepository.executeInTransaction(conn -> {
                    User persisted = userRepository.save(conn, user);
                    
                    if (defaultRoleId != null) {
                        roleRepository.assignRoleToUser(conn, persisted.getId(), defaultRoleId);
                    }
                    return persisted;
                });
                
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
                .orElseThrow(() -> new ValidationException("User not found: " + userId))
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
                .orElseThrow(() -> new ValidationException("User not found: " + username))
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
                    .orElseThrow(() -> new ValidationException("User not found: " + userId));
                
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
                    .orElseThrow(() -> new ValidationException("User not found: " + userId));
                
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
                    .orElseThrow(() -> new ValidationException("User not found: " + userId));
                
                String newSalt = passwordEncoder.generateSalt();
                String newHash = passwordEncoder.encode(newPassword, newSalt);
                user.setPasswordHash(newHash);
                user.setSalt(newSalt);
                
                userRepository.update(user);
                logger.info("Password reset for user: {}", user.getUsername());
            }
        );
    }
    
    /**
     * Update user information (email, phone, realName) - for users to change their own profile
     */
    public void updateUserInfo(Long userId, String email, String phone, String realName) {
        executeWithTemplate(
            PermissionCodes.CHANGE_PROFILE,
            "UPDATE_USER_INFO",
            "User",
            String.valueOf(userId),
            () -> {
                validateNotNull(userId, "User ID");
                // Email validation
                if (email != null && !email.isBlank() && !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                    throw new ValidationException("Invalid email format");
                }
                // Phone validation (optional: simple format check)
                if (phone != null && !phone.isBlank() && !phone.matches("^[0-9+\\-() ]{6,20}$")) {
                    throw new ValidationException("Invalid phone format");
                }
            },
            () -> {
                User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ValidationException("User not found: " + userId));
                
                if (email != null && !email.isBlank()) {
                    user.setEmail(email);
                }
                if (phone != null && !phone.isBlank()) {
                    user.setPhone(phone);
                }
                if (realName != null && !realName.isBlank()) {
                    user.setRealName(realName);
                }
                
                userRepository.update(user);
                logger.info("User info updated for: {}", user.getUsername());
            }
        );
    }
}

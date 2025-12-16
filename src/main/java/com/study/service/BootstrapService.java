package com.study.service;

import com.study.config.PermissionCodes;
import com.study.context.SessionContext;
import com.study.domain.Permission;
import com.study.domain.Role;
import com.study.domain.User;
import com.study.exception.DataNotFoundException;
import com.study.repository.*;
import com.study.security.PasswordEncoder;
import com.study.security.Sha256PasswordEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Bootstrap service to initialize system with default data
 */
public class BootstrapService {
    private static final Logger logger = LoggerFactory.getLogger(BootstrapService.class);
    
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final PasswordEncoder passwordEncoder;
    
    public BootstrapService(UserRepository userRepository,
                           RoleRepository roleRepository,
                           PermissionRepository permissionRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.passwordEncoder = new Sha256PasswordEncoder();
    }
    
    /**
     * Initialize system with default admin user, roles, and permissions
     */
    public void initializeSystem() {
        logger.info("Initializing system...");
        
        // Check if admin exists
        Optional<User> adminOpt = userRepository.findByUsername("admin");
        if (adminOpt.isPresent()) {
            logger.info("System already initialized");
            return;
        }
        
        // Create default permissions
        createDefaultPermissions();
        
        // Create default roles
        Role adminRole = createRole("ADMIN", "Administrator", "Full system access");
        Role userRole = createRole("USER", "Regular User", "Basic user access");
        
        // Assign all permissions to admin role
        assignAllPermissionsToRole(adminRole.getId());
        
        // Assign basic permissions to user role
        assignBasicPermissionsToRole(userRole.getId());
        
        // Create default admin user
        User admin = createAdmin(adminRole.getId());
        
        logger.info("System initialized successfully");
        logger.info("Default admin user: admin / admin123");
    }
    
    private void createDefaultPermissions() {
        String[] permissionCodes = {
            PermissionCodes.USER_CREATE, PermissionCodes.USER_UPDATE, 
            PermissionCodes.USER_DELETE, PermissionCodes.USER_VIEW, 
            PermissionCodes.USER_LIST,
            PermissionCodes.ROLE_CREATE, PermissionCodes.ROLE_UPDATE,
            PermissionCodes.ROLE_DELETE, PermissionCodes.ROLE_VIEW,
            PermissionCodes.ROLE_ASSIGN,
            PermissionCodes.PERMISSION_CREATE, PermissionCodes.PERMISSION_UPDATE,
            PermissionCodes.PERMISSION_DELETE, PermissionCodes.PERMISSION_VIEW,
            PermissionCodes.PERMISSION_ASSIGN,
            PermissionCodes.AUDIT_VIEW, PermissionCodes.AUDIT_VIEW_ALL
        };
        
        for (String code : permissionCodes) {
            if (permissionRepository.findByCode(code).isEmpty()) {
                Permission permission = new Permission();
                permission.setCode(code);
                permission.setName(code.replace("_", " "));
                permission.setDescription("Permission for " + code);
                permissionRepository.save(permission);
                logger.debug("Created permission: {}", code);
            }
        }
    }
    
    private Role createRole(String code, String name, String description) {
        Optional<Role> existing = roleRepository.findByCode(code);
        if (existing.isPresent()) {
            return existing.get();
        }
        
        Role role = new Role();
        role.setCode(code);
        role.setName(name);
        role.setDescription(description);
        Role saved = roleRepository.save(role);
        logger.info("Created role: {}", code);
        return saved;
    }
    
    private void assignAllPermissionsToRole(Long roleId) {
        var allPermissions = permissionRepository.findAll();
        for (Permission permission : allPermissions) {
            try {
                permissionRepository.assignPermissionToRole(roleId, permission.getId());
            } catch (Exception e) {
                // Ignore if already assigned
            }
        }
    }
    
    private void assignBasicPermissionsToRole(Long roleId) {
        String[] basicCodes = {
            PermissionCodes.USER_VIEW, PermissionCodes.USER_LIST,
            PermissionCodes.ROLE_VIEW, PermissionCodes.PERMISSION_VIEW,
            PermissionCodes.AUDIT_VIEW
        };
        
        for (String code : basicCodes) {
            permissionRepository.findByCode(code).ifPresent(permission -> {
                try {
                    permissionRepository.assignPermissionToRole(roleId, permission.getId());
                } catch (Exception e) {
                    // Ignore if already assigned
                }
            });
        }
    }
    
    private User createAdmin(Long adminRoleId) {
        String salt = passwordEncoder.generateSalt();
        String passwordHash = passwordEncoder.encode("admin123", salt);
        
        User admin = new User();
        admin.setUsername("admin");
        admin.setPasswordHash(passwordHash);
        admin.setSalt(salt);
        admin.setEnabled(true);
        
        User saved = userRepository.save(admin);
        roleRepository.assignRoleToUser(saved.getId(), adminRoleId);
        
        logger.info("Created admin user");
        return saved;
    }
}

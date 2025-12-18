package com.study.repository;

import com.study.config.PermissionCodes;
import com.study.domain.Permission;
import com.study.domain.Role;
import com.study.domain.User;
import com.study.security.PasswordEncoder;
import com.study.security.Sha256PasswordEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

/**
 * Database connection manager using MySQL
 */
public class DatabaseConnection {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnection.class);
    private static final String DEFAULT_DB_URL = "jdbc:mysql://localhost:3306/campus_trade"
            + "?useSSL=false&allowPublicKeyRetrieval=true"
            + "&useUnicode=true&characterEncoding=UTF-8"
            + "&serverTimezone=Asia/Shanghai";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "123456";
    
    private static DatabaseConnection instance;
    private final String dbUrl;
    
    private final PasswordEncoder passwordEncoder = new Sha256PasswordEncoder();

    private DatabaseConnection() {
        this.dbUrl = System.getProperty("rbac.db.url", DEFAULT_DB_URL);
        initializeSchema();
    }
    
    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    public static synchronized void reset() {
        instance = null;
    }
    
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl, DB_USER, DB_PASSWORD);
    }
    
    private void initializeSchema() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Create tables
            createTables(stmt);
            logger.info("Database initialized successfully");
        } catch (SQLException e) {
            logger.error("Failed to initialize database", e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }
    
    private void createTables(Statement stmt) throws SQLException {
        // Users table
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS users (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                username VARCHAR(50) UNIQUE NOT NULL,
                password_hash VARCHAR(255) NOT NULL,
                salt VARCHAR(255) NOT NULL,
                enabled BOOLEAN DEFAULT TRUE,
                email VARCHAR(100),
                phone VARCHAR(20),
                real_name VARCHAR(100),
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """);
        
        // Roles table
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS roles (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                code VARCHAR(50) UNIQUE NOT NULL,
                name VARCHAR(100) NOT NULL,
                description VARCHAR(255),
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """);
        
        // Resources table
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS resources (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                code VARCHAR(50) UNIQUE NOT NULL,
                name VARCHAR(100) NOT NULL,
                type VARCHAR(50) NOT NULL,
                url VARCHAR(255),
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """);
        
        // Permissions table
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS permissions (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                code VARCHAR(50) UNIQUE NOT NULL,
                name VARCHAR(100) NOT NULL,
                description VARCHAR(255),
                resource_id BIGINT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (resource_id) REFERENCES resources(id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """);
        
        // User-Role mapping
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS user_roles (
                user_id BIGINT NOT NULL,
                role_id BIGINT NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                PRIMARY KEY (user_id, role_id),
                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """);
        
        // Role-Permission mapping
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS role_permissions (
                role_id BIGINT NOT NULL,
                permission_id BIGINT NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                PRIMARY KEY (role_id, permission_id),
                FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
                FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """);
        
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS role_permission_scopes (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                role_id BIGINT NOT NULL,
                permission_code VARCHAR(50) NOT NULL,
                resource_type VARCHAR(50) NOT NULL,
                resource_id VARCHAR(100),
                scope_key VARCHAR(100) NOT NULL DEFAULT '__GLOBAL__',
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
                FOREIGN KEY (permission_code) REFERENCES permissions(code) ON DELETE CASCADE,
                UNIQUE KEY uq_role_permission_scope (role_id, permission_code, resource_type, scope_key)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """);
        
        // Audit logs
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS audit_logs (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                user_id BIGINT,
                username VARCHAR(50),
                action VARCHAR(100) NOT NULL,
                resource_type VARCHAR(50),
                resource_id VARCHAR(100),
                detail TEXT,
                success BOOLEAN NOT NULL,
                error_message TEXT,
                ip_address VARCHAR(50),
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """);
    }
    
    /**
     * Initialize default roles/permissions/admin user.
     * Idempotent: safe to call multiple times.
     */
    public void initializeDefaults() {
        UserRepository userRepository = new UserRepository(this);
        RoleRepository roleRepository = new RoleRepository(this);
        PermissionRepository permissionRepository = new PermissionRepository(this);

        Optional<User> adminOpt = userRepository.findByUsername("admin");
        if (adminOpt.isPresent()) {
            logger.info("System already initialized");
            return;
        }

        logger.info("Initializing system defaults...");
        createDefaultPermissions(permissionRepository);
        Role adminRole = ensureRole(roleRepository, "ADMIN", "Administrator", "Full system access");
        Role userRole = ensureRole(roleRepository, "USER", "Regular User", "Basic user access");

        assignAllPermissionsToRole(permissionRepository, adminRole.getId());
        assignBasicPermissionsToRole(permissionRepository, userRole.getId());
        createAdminUser(userRepository, roleRepository, adminRole.getId());

        logger.info("Default data initialized. Admin: admin / admin123");
    }

    private void createDefaultPermissions(PermissionRepository permissionRepository) {
        String[] permissionCodes = {
                PermissionCodes.USER_CREATE, PermissionCodes.USER_UPDATE,
                PermissionCodes.USER_DELETE, PermissionCodes.USER_VIEW,
                PermissionCodes.USER_LIST, PermissionCodes.CHANGE_PROFILE,
                PermissionCodes.ROLE_CREATE, PermissionCodes.ROLE_UPDATE,
                PermissionCodes.ROLE_DELETE, PermissionCodes.ROLE_VIEW,
                PermissionCodes.ROLE_ASSIGN,
                PermissionCodes.PERMISSION_CREATE, PermissionCodes.PERMISSION_UPDATE,
                PermissionCodes.PERMISSION_DELETE, PermissionCodes.PERMISSION_VIEW,
                PermissionCodes.PERMISSION_ASSIGN,
                PermissionCodes.RESOURCE_CREATE, PermissionCodes.RESOURCE_UPDATE,
                PermissionCodes.RESOURCE_DELETE, PermissionCodes.RESOURCE_VIEW,
                PermissionCodes.RESOURCE_LIST, PermissionCodes.RESOURCE_GRANT,
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

    private Role ensureRole(RoleRepository roleRepository, String code, String name, String description) {
        Optional<Role> existing = roleRepository.findByCode(code);
        if (existing.isPresent()) {
            return existing.get();
        }
        Role role = new Role();
        role.setCode(code);
        role.setName(name);
        role.setDescription(description);
        return roleRepository.save(role);
    }

    private void assignAllPermissionsToRole(PermissionRepository permissionRepository, Long roleId) {
        var allPermissions = permissionRepository.findAll();
        for (Permission permission : allPermissions) {
            try {
                permissionRepository.assignPermissionToRole(roleId, permission.getId());
            } catch (Exception ignored) {
                // ignore duplicates
            }
        }
    }

    private void assignBasicPermissionsToRole(PermissionRepository permissionRepository, Long roleId) {
        String[] basicCodes = {
                PermissionCodes.USER_VIEW, PermissionCodes.USER_LIST,
                PermissionCodes.CHANGE_PROFILE,
                PermissionCodes.ROLE_VIEW, PermissionCodes.PERMISSION_VIEW,
                PermissionCodes.AUDIT_VIEW,
                PermissionCodes.RESOURCE_VIEW,
                PermissionCodes.RESOURCE_LIST
        };

        for (String code : basicCodes) {
            permissionRepository.findByCode(code).ifPresent(permission -> {
                try {
                    permissionRepository.assignPermissionToRole(roleId, permission.getId());
                } catch (Exception ignored) {
                }
            });
        }
    }

    private void createAdminUser(UserRepository userRepository, RoleRepository roleRepository, Long adminRoleId) {
        String salt = passwordEncoder.generateSalt();
        String passwordHash = passwordEncoder.encode("admin123", salt);

        User admin = new User();
        admin.setUsername("admin");
        admin.setPasswordHash(passwordHash);
        admin.setSalt(salt);
        admin.setEnabled(true);

        User saved = userRepository.save(admin);
        roleRepository.assignRoleToUser(saved.getId(), adminRoleId);
    }
}

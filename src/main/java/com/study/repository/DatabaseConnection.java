package com.study.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

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
    
    private DatabaseConnection() {
        this.dbUrl = System.getProperty("rbac.db.url", DEFAULT_DB_URL);
        initializeDatabase();
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
    
    private void initializeDatabase() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Create tables
            createTables(stmt);
            // Initialize default data
            initializeDefaultData(stmt);
            
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
    
    private void initializeDefaultData(Statement stmt) throws SQLException {
        // Check if admin user exists
        var rs = stmt.executeQuery("SELECT COUNT(*) FROM users WHERE username = 'admin'");
        if (rs.next() && rs.getInt(1) == 0) {
            // Will be initialized by bootstrap service
            logger.info("No default data found, will be initialized on first run");
        }
    }
}

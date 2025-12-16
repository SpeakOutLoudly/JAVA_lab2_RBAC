package com.study.repository;

import com.study.domain.Permission;
import com.study.exception.DataNotFoundException;
import com.study.exception.DuplicateDataException;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PermissionRepository extends BaseRepository {
    
    public PermissionRepository(DatabaseConnection dbConnection) {
        super(dbConnection);
    }
    
    public Permission save(Permission permission) {
        String sql = "INSERT INTO permissions (code, name, description, resource_id) VALUES (?, ?, ?, ?)";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, permission.getCode());
            pstmt.setString(2, permission.getName());
            pstmt.setString(3, permission.getDescription());
            if (permission.getResourceId() != null) {
                pstmt.setLong(4, permission.getResourceId());
            } else {
                pstmt.setNull(4, Types.BIGINT);
            }
            
            pstmt.executeUpdate();
            
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    permission.setId(generatedKeys.getLong(1));
                }
            }
            
            return permission;
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new DuplicateDataException("Permission code already exists: " + permission.getCode());
        } catch (SQLException e) {
            logger.error("Failed to save permission", e);
            throw new RuntimeException("Failed to save permission", e);
        }
    }
    
    public Optional<Permission> findById(Long id) {
        String sql = "SELECT * FROM permissions WHERE id = ?";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, id);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return Optional.of(mapResultSetToPermission(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            logger.error("Failed to find permission", e);
            throw new RuntimeException("Failed to find permission", e);
        }
    }
    
    public Optional<Permission> findByCode(String code) {
        String sql = "SELECT * FROM permissions WHERE code = ?";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, code);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return Optional.of(mapResultSetToPermission(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            logger.error("Failed to find permission by code", e);
            throw new RuntimeException("Failed to find permission", e);
        }
    }
    
    public List<Permission> findAll() {
        String sql = "SELECT * FROM permissions ORDER BY created_at DESC";
        List<Permission> permissions = new ArrayList<>();
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                permissions.add(mapResultSetToPermission(rs));
            }
            return permissions;
        } catch (SQLException e) {
            logger.error("Failed to find all permissions", e);
            throw new RuntimeException("Failed to find permissions", e);
        }
    }
    
    public List<Permission> findByRoleId(Long roleId) {
        String sql = """
            SELECT p.* FROM permissions p
            INNER JOIN role_permissions rp ON p.id = rp.permission_id
            WHERE rp.role_id = ?
        """;
        List<Permission> permissions = new ArrayList<>();
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, roleId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                permissions.add(mapResultSetToPermission(rs));
            }
            return permissions;
        } catch (SQLException e) {
            logger.error("Failed to find permissions for role", e);
            throw new RuntimeException("Failed to find permissions", e);
        }
    }
    
    public List<Permission> findByUserId(Long userId) {
        String sql = """
            SELECT DISTINCT p.* FROM permissions p
            INNER JOIN role_permissions rp ON p.id = rp.permission_id
            INNER JOIN user_roles ur ON rp.role_id = ur.role_id
            WHERE ur.user_id = ?
        """;
        List<Permission> permissions = new ArrayList<>();
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                permissions.add(mapResultSetToPermission(rs));
            }
            return permissions;
        } catch (SQLException e) {
            logger.error("Failed to find permissions for user", e);
            throw new RuntimeException("Failed to find permissions", e);
        }
    }
    
    public void assignPermissionToRole(Long roleId, Long permissionId) {
        String sql = "INSERT INTO role_permissions (role_id, permission_id) VALUES (?, ?)";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, roleId);
            pstmt.setLong(2, permissionId);
            pstmt.executeUpdate();
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new DuplicateDataException("Role already has this permission");
        } catch (SQLException e) {
            logger.error("Failed to assign permission to role", e);
            throw new RuntimeException("Failed to assign permission", e);
        }
    }
    
    public void removePermissionFromRole(Long roleId, Long permissionId) {
        String sql = "DELETE FROM role_permissions WHERE role_id = ? AND permission_id = ?";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, roleId);
            pstmt.setLong(2, permissionId);
            int affected = pstmt.executeUpdate();
            
            if (affected == 0) {
                throw new DataNotFoundException("Role does not have this permission");
            }
        } catch (SQLException e) {
            logger.error("Failed to remove permission from role", e);
            throw new RuntimeException("Failed to remove permission", e);
        }
    }
    
    private Permission mapResultSetToPermission(ResultSet rs) throws SQLException {
        Permission permission = new Permission();
        permission.setId(rs.getLong("id"));
        permission.setCode(rs.getString("code"));
        permission.setName(rs.getString("name"));
        permission.setDescription(rs.getString("description"));
        long resourceId = rs.getLong("resource_id");
        if (!rs.wasNull()) {
            permission.setResourceId(resourceId);
        }
        permission.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return permission;
    }
}

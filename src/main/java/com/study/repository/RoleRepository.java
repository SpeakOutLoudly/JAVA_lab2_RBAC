package com.study.repository;

import com.study.domain.Role;
import com.study.exception.DataNotFoundException;
import com.study.exception.DuplicateDataException;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RoleRepository extends BaseRepository {
    
    public RoleRepository(DatabaseConnection dbConnection) {
        super(dbConnection);
    }
    
    public Role save(Role role) {
        String sql = "INSERT INTO roles (code, name, description) VALUES (?, ?, ?)";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, role.getCode());
            pstmt.setString(2, role.getName());
            pstmt.setString(3, role.getDescription());
            
            pstmt.executeUpdate();
            
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    role.setId(generatedKeys.getLong(1));
                }
            }
            
            return role;
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new DuplicateDataException("Role code already exists: " + role.getCode());
        } catch (SQLException e) {
            logger.error("Failed to save role", e);
            throw new RuntimeException("Failed to save role", e);
        }
    }
    
    public Optional<Role> findById(Long id) {
        String sql = "SELECT * FROM roles WHERE id = ?";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, id);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return Optional.of(mapResultSetToRole(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            logger.error("Failed to find role", e);
            throw new RuntimeException("Failed to find role", e);
        }
    }
    
    public Optional<Role> findByCode(String code) {
        String sql = "SELECT * FROM roles WHERE code = ?";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, code);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return Optional.of(mapResultSetToRole(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            logger.error("Failed to find role by code", e);
            throw new RuntimeException("Failed to find role", e);
        }
    }
    
    public List<Role> findAll() {
        String sql = "SELECT * FROM roles ORDER BY created_at DESC";
        List<Role> roles = new ArrayList<>();
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                roles.add(mapResultSetToRole(rs));
            }
            return roles;
        } catch (SQLException e) {
            logger.error("Failed to find all roles", e);
            throw new RuntimeException("Failed to find roles", e);
        }
    }
    
    public List<Role> findByUserId(Long userId) {
        String sql = """
            SELECT r.* FROM roles r
            INNER JOIN user_roles ur ON r.id = ur.role_id
            WHERE ur.user_id = ?
        """;
        List<Role> roles = new ArrayList<>();
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                roles.add(mapResultSetToRole(rs));
            }
            return roles;
        } catch (SQLException e) {
            logger.error("Failed to find roles for user", e);
            throw new RuntimeException("Failed to find roles", e);
        }
    }
    
    public void assignRoleToUser(Long userId, Long roleId) {
        String sql = "INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, userId);
            pstmt.setLong(2, roleId);
            pstmt.executeUpdate();
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new DuplicateDataException("User already has this role");
        } catch (SQLException e) {
            logger.error("Failed to assign role to user", e);
            throw new RuntimeException("Failed to assign role", e);
        }
    }
    
    public void removeRoleFromUser(Long userId, Long roleId) {
        String sql = "DELETE FROM user_roles WHERE user_id = ? AND role_id = ?";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, userId);
            pstmt.setLong(2, roleId);
            int affected = pstmt.executeUpdate();
            
            if (affected == 0) {
                throw new DataNotFoundException("User does not have this role");
            }
        } catch (SQLException e) {
            logger.error("Failed to remove role from user", e);
            throw new RuntimeException("Failed to remove role", e);
        }
    }
    
    private Role mapResultSetToRole(ResultSet rs) throws SQLException {
        Role role = new Role();
        role.setId(rs.getLong("id"));
        role.setCode(rs.getString("code"));
        role.setName(rs.getString("name"));
        role.setDescription(rs.getString("description"));
        role.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return role;
    }
}

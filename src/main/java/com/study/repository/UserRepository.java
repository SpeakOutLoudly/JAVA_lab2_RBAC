package com.study.repository;

import com.study.domain.User;
import com.study.exception.DataNotFoundException;
import com.study.exception.DuplicateDataException;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repository for User entity
 */
public class UserRepository extends BaseRepository {
    
    public UserRepository(DatabaseConnection dbConnection) {
        super(dbConnection);
    }
    
    public User save(User user) {
        String sql = """
            INSERT INTO users (username, password_hash, salt, enabled, created_at, updated_at)
            VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """;
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getPasswordHash());
            pstmt.setString(3, user.getSalt());
            pstmt.setBoolean(4, user.isEnabled());
            
            int affected = pstmt.executeUpdate();
            if (affected == 0) {
                throw new RuntimeException("Creating user failed, no rows affected");
            }
            
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    user.setId(generatedKeys.getLong(1));
                }
            }
            
            return user;
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new DuplicateDataException("Username already exists: " + user.getUsername());
        } catch (SQLException e) {
            logger.error("Failed to save user", e);
            throw new RuntimeException("Failed to save user", e);
        }
    }
    
    public Optional<User> findById(Long id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, id);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return Optional.of(mapResultSetToUser(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            logger.error("Failed to find user by id", e);
            throw new RuntimeException("Failed to find user", e);
        }
    }
    
    public Optional<User> findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return Optional.of(mapResultSetToUser(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            logger.error("Failed to find user by username", e);
            throw new RuntimeException("Failed to find user", e);
        }
    }
    
    public List<User> findAll() {
        String sql = "SELECT * FROM users ORDER BY created_at DESC";
        List<User> users = new ArrayList<>();
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
            return users;
        } catch (SQLException e) {
            logger.error("Failed to find all users", e);
            throw new RuntimeException("Failed to find users", e);
        }
    }
    
    public void update(User user) {
        String sql = """
            UPDATE users SET username = ?, password_hash = ?, salt = ?, 
            enabled = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?
        """;
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getPasswordHash());
            pstmt.setString(3, user.getSalt());
            pstmt.setBoolean(4, user.isEnabled());
            pstmt.setLong(5, user.getId());
            
            int affected = pstmt.executeUpdate();
            if (affected == 0) {
                throw new DataNotFoundException("User not found: " + user.getId());
            }
        } catch (SQLException e) {
            logger.error("Failed to update user", e);
            throw new RuntimeException("Failed to update user", e);
        }
    }
    
    public void delete(Long id) {
        String sql = "DELETE FROM users WHERE id = ?";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, id);
            int affected = pstmt.executeUpdate();
            if (affected == 0) {
                throw new DataNotFoundException("User not found: " + id);
            }
        } catch (SQLException e) {
            logger.error("Failed to delete user", e);
            throw new RuntimeException("Failed to delete user", e);
        }
    }
    
    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setUsername(rs.getString("username"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setSalt(rs.getString("salt"));
        user.setEnabled(rs.getBoolean("enabled"));
        user.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        user.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        return user;
    }
}

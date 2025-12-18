package com.study.repository;

import com.study.domain.User;
import com.study.exception.DataAccessException;
import com.study.exception.ValidationException;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repository for User entity.
 */
public class UserRepository extends BaseRepository {

    public UserRepository(DatabaseConnection dbConnection) {
        super(dbConnection);
    }

    public User save(User user) {
        try (Connection conn = dbConnection.getConnection()) {
            return save(conn, user);
        } catch (SQLException e) {
            logger.error("Failed to save user", e);
            throw new DataAccessException("Failed to save user", e);
        }
    }

    public User save(Connection conn, User user) {
        String sql = """
            INSERT INTO users (username, password_hash, salt, enabled, email, phone, real_name, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """;

        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getPasswordHash());
            pstmt.setString(3, user.getSalt());
            pstmt.setBoolean(4, user.isEnabled());
            pstmt.setString(5, user.getEmail());
            pstmt.setString(6, user.getPhone());
            pstmt.setString(7, user.getRealName());

            int affected = pstmt.executeUpdate();
            if (affected == 0) {
                throw new DataAccessException("Creating user failed, no rows affected");
            }

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    user.setId(generatedKeys.getLong(1));
                }
            }
            return user;
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new ValidationException("Username already exists: " + user.getUsername());
        } catch (SQLException e) {
            throw new DataAccessException("Failed to save user", e);
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
            throw new DataAccessException("Failed to find user", e);
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
            throw new DataAccessException("Failed to find user", e);
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
            logger.error("Failed to list users", e);
            throw new DataAccessException("Failed to list users", e);
        }
    }

    public void update(User user) {
        try (Connection conn = dbConnection.getConnection()) {
            update(conn, user);
        } catch (SQLException e) {
            logger.error("Failed to update user", e);
            throw new DataAccessException("Failed to update user", e);
        }
    }

    public void update(Connection conn, User user) {
        String sql = """
            UPDATE users SET username = ?, password_hash = ?, salt = ?,
            enabled = ?, email = ?, phone = ?, real_name = ?,
            updated_at = CURRENT_TIMESTAMP WHERE id = ?
        """;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getPasswordHash());
            pstmt.setString(3, user.getSalt());
            pstmt.setBoolean(4, user.isEnabled());
            pstmt.setString(5, user.getEmail());
            pstmt.setString(6, user.getPhone());
            pstmt.setString(7, user.getRealName());
            pstmt.setLong(8, user.getId());

            int affected = pstmt.executeUpdate();
            if (affected == 0) {
                throw new ValidationException("User not found: " + user.getId());
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update user", e);
        }
    }

    public void delete(Long id) {
        try (Connection conn = dbConnection.getConnection()) {
            delete(conn, id);
        } catch (SQLException e) {
            logger.error("Failed to delete user", e);
            throw new DataAccessException("Failed to delete user", e);
        }
    }

    public void delete(Connection conn, Long id) {
        String sql = "DELETE FROM users WHERE id = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            int affected = pstmt.executeUpdate();
            if (affected == 0) {
                throw new ValidationException("User not found: " + id);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete user", e);
        }
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setUsername(rs.getString("username"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setSalt(rs.getString("salt"));
        user.setEnabled(rs.getBoolean("enabled"));
        user.setEmail(rs.getString("email"));
        user.setPhone(rs.getString("phone"));
        user.setRealName(rs.getString("real_name"));
        user.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        user.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        return user;
    }
}

package com.study.repository;

import com.study.domain.Role;
import com.study.exception.DataAccessException;
import com.study.exception.ValidationException;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repository for role entity.
 */
public class RoleRepository extends BaseRepository {

    public RoleRepository(DatabaseConnection dbConnection) {
        super(dbConnection);
    }

    public Role save(Role role) {
        try (Connection conn = dbConnection.getConnection()) {
            return save(conn, role);
        } catch (SQLException e) {
            logger.error("Failed to save role", e);
            throw new DataAccessException("Failed to save role", e);
        }
    }

    public Role save(Connection conn, Role role) {
        String sql = "INSERT INTO roles (code, name, description) VALUES (?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, role.getCode());
            pstmt.setString(2, role.getName());
            pstmt.setString(3, role.getDescription());
            pstmt.executeUpdate();

            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()) {
                    role.setId(keys.getLong(1));
                }
            }
            return role;
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new ValidationException("Role code already exists: " + role.getCode());
        } catch (SQLException e) {
            throw new DataAccessException("Failed to save role", e);
        }
    }

    public void update(Role role) {
        try (Connection conn = dbConnection.getConnection()) {
            update(conn, role);
        } catch (SQLException e) {
            logger.error("Failed to update role", e);
            throw new DataAccessException("Failed to update role", e);
        }
    }

    public void update(Connection conn, Role role) {
        String sql = "UPDATE roles SET name = ?, description = ? WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, role.getName());
            pstmt.setString(2, role.getDescription());
            pstmt.setLong(3, role.getId());
            if (pstmt.executeUpdate() == 0) {
                throw new ValidationException("Role not found: " + role.getId());
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update role", e);
        }
    }

    public void delete(Long roleId) {
        try (Connection conn = dbConnection.getConnection()) {
            delete(conn, roleId);
        } catch (SQLException e) {
            logger.error("Failed to delete role", e);
            throw new DataAccessException("Failed to delete role", e);
        }
    }

    public void delete(Connection conn, Long roleId) {
        String sql = "DELETE FROM roles WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, roleId);
            if (pstmt.executeUpdate() == 0) {
                throw new ValidationException("Role not found: " + roleId);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete role", e);
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
            logger.error("Failed to find role by id", e);
            throw new DataAccessException("Failed to find role", e);
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
            throw new DataAccessException("Failed to find role", e);
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
            logger.error("Failed to list roles", e);
            throw new DataAccessException("Failed to list roles", e);
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
            throw new DataAccessException("Failed to find roles", e);
        }
    }

    public void assignRoleToUser(Long userId, Long roleId) {
        try (Connection conn = dbConnection.getConnection()) {
            assignRoleToUser(conn, userId, roleId);
        } catch (SQLException e) {
            logger.error("Failed to assign role to user", e);
            throw new DataAccessException("Failed to assign role", e);
        }
    }

    public void assignRoleToUser(Connection conn, Long userId, Long roleId) {
        String sql = "INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            pstmt.setLong(2, roleId);
            pstmt.executeUpdate();
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new ValidationException("User already has this role");
        } catch (SQLException e) {
            throw new DataAccessException("Failed to assign role", e);
        }
    }

    public void removeRoleFromUser(Long userId, Long roleId) {
        try (Connection conn = dbConnection.getConnection()) {
            removeRoleFromUser(conn, userId, roleId);
        } catch (SQLException e) {
            logger.error("Failed to remove role from user", e);
            throw new DataAccessException("Failed to remove role", e);
        }
    }

    public void removeRoleFromUser(Connection conn, Long userId, Long roleId) {
        String sql = "DELETE FROM user_roles WHERE user_id = ? AND role_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            pstmt.setLong(2, roleId);
            if (pstmt.executeUpdate() == 0) {
                throw new ValidationException("User does not have this role");
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to remove role", e);
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

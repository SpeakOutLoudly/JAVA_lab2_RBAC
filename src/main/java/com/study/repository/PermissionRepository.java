package com.study.repository;

import com.study.domain.Permission;
import com.study.domain.ScopedPermission;
import com.study.service.dto.ResourceRoleScope;
import com.study.service.dto.ResourceUserScope;
import com.study.exception.DataAccessException;
import com.study.exception.ValidationException;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repository for permission entity.
 */
public class PermissionRepository extends BaseRepository {

    public PermissionRepository(DatabaseConnection dbConnection) {
        super(dbConnection);
    }

    public Permission save(Permission permission) {
        try (Connection conn = dbConnection.getConnection()) {
            return save(conn, permission);
        } catch (SQLException e) {
            logger.error("Failed to save permission", e);
            throw new DataAccessException("Failed to save permission", e);
        }
    }

    public Permission save(Connection conn, Permission permission) {
        String sql = "INSERT INTO permissions (code, name, description, resource_id) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, permission.getCode());
            pstmt.setString(2, permission.getName());
            pstmt.setString(3, permission.getDescription());
            if (permission.getResourceId() != null) {
                pstmt.setLong(4, permission.getResourceId());
            } else {
                pstmt.setNull(4, Types.BIGINT);
            }
            pstmt.executeUpdate();

            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()) {
                    permission.setId(keys.getLong(1));
                }
            }
            return permission;
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new ValidationException("Permission code already exists: " + permission.getCode());
        } catch (SQLException e) {
            throw new DataAccessException("Failed to save permission", e);
        }
    }

    public void update(Permission permission) {
        try (Connection conn = dbConnection.getConnection()) {
            update(conn, permission);
        } catch (SQLException e) {
            logger.error("Failed to update permission", e);
            throw new DataAccessException("Failed to update permission", e);
        }
    }

    public void update(Connection conn, Permission permission) {
        String sql = "UPDATE permissions SET name = ?, description = ?, resource_id = ? WHERE code = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, permission.getName());
            pstmt.setString(2, permission.getDescription());
            if (permission.getResourceId() != null) {
                pstmt.setLong(3, permission.getResourceId());
            } else {
                pstmt.setNull(3, Types.BIGINT);
            }
            pstmt.setString(4, permission.getCode());
            if (pstmt.executeUpdate() == 0) {
                throw new ValidationException("Permission not found: " + permission.getCode());
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update permission", e);
        }
    }

    public void delete(Long permissionId) {
        try (Connection conn = dbConnection.getConnection()) {
            delete(conn, permissionId);
        } catch (SQLException e) {
            logger.error("Failed to delete permission", e);
            throw new DataAccessException("Failed to delete permission", e);
        }
    }

    public void delete(Connection conn, Long permissionId) {
        String sql = "DELETE FROM permissions WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, permissionId);
            if (pstmt.executeUpdate() == 0) {
                throw new ValidationException("Permission not found: " + permissionId);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete permission", e);
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
            logger.error("Failed to find permission by id", e);
            throw new DataAccessException("Failed to find permission", e);
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
            throw new DataAccessException("Failed to find permission", e);
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
            logger.error("Failed to list permissions", e);
            throw new DataAccessException("Failed to list permissions", e);
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
            throw new DataAccessException("Failed to find permissions for role", e);
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
            throw new DataAccessException("Failed to find permissions for user", e);
        }
    }

    public void assignPermissionToRole(Long roleId, Long permissionId) {
        try (Connection conn = dbConnection.getConnection()) {
            assignPermissionToRole(conn, roleId, permissionId);
        } catch (SQLException e) {
            logger.error("Failed to assign permission to role", e);
            throw new DataAccessException("Failed to assign permission", e);
        }
    }

    public void assignPermissionToRole(Connection conn, Long roleId, Long permissionId) {
        String sql = "INSERT INTO role_permissions (role_id, permission_id) VALUES (?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, roleId);
            pstmt.setLong(2, permissionId);
            pstmt.executeUpdate();
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new ValidationException("Role already has this permission");
        } catch (SQLException e) {
            throw new DataAccessException("Failed to assign permission", e);
        }
    }

    public void removePermissionFromRole(Long roleId, Long permissionId) {
        try (Connection conn = dbConnection.getConnection()) {
            removePermissionFromRole(conn, roleId, permissionId);
        } catch (SQLException e) {
            logger.error("Failed to remove permission from role", e);
            throw new DataAccessException("Failed to remove permission", e);
        }
    }

    public void removePermissionFromRole(Connection conn, Long roleId, Long permissionId) {
        String sql = "DELETE FROM role_permissions WHERE role_id = ? AND permission_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, roleId);
            pstmt.setLong(2, permissionId);
            if (pstmt.executeUpdate() == 0) {
                throw new ValidationException("Role does not have this permission");
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to remove permission", e);
        }
    }

    public void assignScopedPermission(Long roleId, String permissionCode, String resourceType, String resourceId) {
        try (Connection conn = dbConnection.getConnection()) {
            assignScopedPermission(conn, roleId, permissionCode, resourceType, resourceId);
        } catch (SQLException e) {
            logger.error("Failed to assign scoped permission", e);
            throw new DataAccessException("Failed to assign scoped permission", e);
        }
    }

    public void assignScopedPermission(Connection conn, Long roleId, String permissionCode,
                                       String resourceType, String resourceId) {
        String sql = """
            INSERT INTO role_permission_scopes (role_id, permission_code, resource_type, resource_id, scope_key)
            VALUES (?, ?, ?, ?, ?)
        """;
        String scopeKey = (resourceId != null && !resourceId.isBlank()) ? resourceId : "__GLOBAL__";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, roleId);
            pstmt.setString(2, permissionCode);
            pstmt.setString(3, resourceType);
            if (resourceId != null && !resourceId.isBlank()) {
                pstmt.setString(4, resourceId);
            } else {
                pstmt.setNull(4, Types.VARCHAR);
            }
            pstmt.setString(5, scopeKey);
            pstmt.executeUpdate();
        } catch (SQLIntegrityConstraintViolationException e) {
            int errorCode = e.getErrorCode();
            if (errorCode == 1062) { // duplicate key
                throw new ValidationException("Scoped permission already exists for this role");
            }
            if (errorCode == 1452) { // foreign key constraint fails
                throw new ValidationException("Role or permission not found for scoped permission");
            }
            throw new DataAccessException("Failed to assign scoped permission", e);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to assign scoped permission", e);
        }
    }

    public void removeScopedPermission(Long roleId, String permissionCode, String resourceType, String resourceId) {
        try (Connection conn = dbConnection.getConnection()) {
            removeScopedPermission(conn, roleId, permissionCode, resourceType, resourceId);
        } catch (SQLException e) {
            logger.error("Failed to remove scoped permission", e);
            throw new DataAccessException("Failed to remove scoped permission", e);
        }
    }

    public void removeScopedPermission(Connection conn, Long roleId, String permissionCode,
                                       String resourceType, String resourceId) {
        String sql = """
            DELETE FROM role_permission_scopes
            WHERE role_id = ? AND permission_code = ? AND resource_type = ? AND scope_key = ?
        """;
        String scopeKey = (resourceId != null && !resourceId.isBlank()) ? resourceId : "__GLOBAL__";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, roleId);
            pstmt.setString(2, permissionCode);
            pstmt.setString(3, resourceType);
            pstmt.setString(4, scopeKey);
            if (pstmt.executeUpdate() == 0) {
                throw new ValidationException("Scoped permission not found for role");
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to remove scoped permission", e);
        }
    }

    public List<ScopedPermission> findScopedPermissionsByUserId(Long userId) {
        String sql = """
            SELECT rps.role_id, rps.permission_code, rps.resource_type, rps.resource_id
            FROM role_permission_scopes rps
            INNER JOIN user_roles ur ON ur.role_id = rps.role_id
            WHERE ur.user_id = ?
        """;
        List<ScopedPermission> scopedPermissions = new ArrayList<>();
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                scopedPermissions.add(mapScopedPermission(rs));
            }
            return scopedPermissions;
        } catch (SQLException e) {
            logger.error("Failed to find scoped permissions for user", e);
            throw new DataAccessException("Failed to find scoped permissions for user", e);
        }
    }

    public List<ScopedPermission> findScopedPermissionsByRoleId(Long roleId) {
        String sql = """
            SELECT rps.role_id, rps.permission_code, rps.resource_type, rps.resource_id
            FROM role_permission_scopes rps
            WHERE rps.role_id = ?
        """;
        List<ScopedPermission> scopedPermissions = new ArrayList<>();
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, roleId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                scopedPermissions.add(mapScopedPermission(rs));
            }
            return scopedPermissions;
        } catch (SQLException e) {
            logger.error("Failed to find scoped permissions for role", e);
            throw new DataAccessException("Failed to find scoped permissions for role", e);
        }
    }

    /**
     * Find roles that have scoped permissions for a specific resource
     */
    public List<ResourceRoleScope> findRoleScopesByResource(Long resourceId) {
        String sql = """
            SELECT r.code AS role_code,
                   s.permission_code,
                   s.resource_id AS scope_key
            FROM role_permission_scopes s
            JOIN roles r ON s.role_id = r.id
            WHERE s.resource_type = 'RESOURCE'
              AND s.resource_id = ?
        """;
        List<ResourceRoleScope> roleScopes = new ArrayList<>();
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, String.valueOf(resourceId));
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                ResourceRoleScope scope = new ResourceRoleScope();
                scope.setRoleCode(rs.getString("role_code"));
                scope.setPermissionCode(rs.getString("permission_code"));
                scope.setScopeKey(rs.getString("scope_key"));
                roleScopes.add(scope);
            }
            return roleScopes;
        } catch (SQLException e) {
            logger.error("Failed to find role scopes by resource", e);
            throw new DataAccessException("Failed to find role scopes by resource", e);
        }
    }

    /**
     * Find users that have scoped permissions for a specific resource
     */
    public List<ResourceUserScope> findUserScopesByResource(Long resourceId) {
        String sql = """
            SELECT DISTINCT u.username AS username,
                   r.code AS role_code,
                   s.permission_code
            FROM role_permission_scopes s
            JOIN roles r ON s.role_id = r.id
            JOIN user_roles ur ON ur.role_id = r.id
            JOIN users u ON ur.user_id = u.id
            WHERE s.resource_type = 'RESOURCE'
              AND s.resource_id = ?
        """;
        List<ResourceUserScope> userScopes = new ArrayList<>();
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, String.valueOf(resourceId));
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                ResourceUserScope scope = new ResourceUserScope();
                scope.setUsername(rs.getString("username"));
                scope.setRoleCode(rs.getString("role_code"));
                scope.setPermissionCode(rs.getString("permission_code"));
                userScopes.add(scope);
            }
            return userScopes;
        } catch (SQLException e) {
            logger.error("Failed to find user scopes by resource", e);
            throw new DataAccessException("Failed to find user scopes by resource", e);
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

    private ScopedPermission mapScopedPermission(ResultSet rs) throws SQLException {
        ScopedPermission scopedPermission = new ScopedPermission();
        scopedPermission.setRoleId(rs.getLong("role_id"));
        scopedPermission.setPermissionCode(rs.getString("permission_code"));
        scopedPermission.setResourceType(rs.getString("resource_type"));
        scopedPermission.setResourceId(rs.getString("resource_id"));
        return scopedPermission;
    }

    public void clearScopedPermissions(Long roleId, String permissionCode, String resourceType) {
        String sql = """
            DELETE FROM role_permission_scopes
            WHERE role_id = ? AND permission_code = ? AND resource_type = ?
        """;
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, roleId);
            pstmt.setString(2, permissionCode);
            pstmt.setString(3, resourceType);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to clear scoped permissions", e);
        }
    }
}

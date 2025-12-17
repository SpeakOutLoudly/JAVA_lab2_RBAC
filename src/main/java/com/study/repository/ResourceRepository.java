package com.study.repository;

import com.study.domain.Resource;
import com.study.exception.DataAccessException;
import com.study.exception.DataNotFoundException;
import com.study.exception.DuplicateKeyException;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ResourceRepository extends BaseRepository {

    public ResourceRepository(DatabaseConnection dbConnection) {
        super(dbConnection);
    }

    public Resource save(Resource resource) {
        try (Connection conn = dbConnection.getConnection()) {
            return save(conn, resource);
        } catch (SQLException e) {
            logger.error("Failed to save resource", e);
            throw new DataAccessException("Failed to save resource", e);
        }
    }

    public Resource save(Connection conn, Resource resource) {
        String sql = "INSERT INTO resources (code, name, type, url) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, resource.getCode());
            pstmt.setString(2, resource.getName());
            pstmt.setString(3, resource.getType());
            pstmt.setString(4, resource.getUrl());
            pstmt.executeUpdate();

            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()) {
                    resource.setId(keys.getLong(1));
                }
            }
            return resource;
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new DuplicateKeyException("Resource code already exists: " + resource.getCode());
        } catch (SQLException e) {
            throw new DataAccessException("Failed to save resource", e);
        }
    }

    public void update(Resource resource) {
        try (Connection conn = dbConnection.getConnection()) {
            update(conn, resource);
        } catch (SQLException e) {
            logger.error("Failed to update resource", e);
            throw new DataAccessException("Failed to update resource", e);
        }
    }

    public void update(Connection conn, Resource resource) {
        String sql = "UPDATE resources SET name = ?, type = ?, url = ? WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, resource.getName());
            pstmt.setString(2, resource.getType());
            pstmt.setString(3, resource.getUrl());
            pstmt.setLong(4, resource.getId());
            if (pstmt.executeUpdate() == 0) {
                throw new DataNotFoundException("Resource not found: " + resource.getId());
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update resource", e);
        }
    }

    public void delete(Long resourceId) {
        try (Connection conn = dbConnection.getConnection()) {
            delete(conn, resourceId);
        } catch (SQLException e) {
            logger.error("Failed to delete resource", e);
            throw new DataAccessException("Failed to delete resource", e);
        }
    }

    public void delete(Connection conn, Long resourceId) {
        String sql = "DELETE FROM resources WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, resourceId);
            if (pstmt.executeUpdate() == 0) {
                throw new DataNotFoundException("Resource not found: " + resourceId);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete resource", e);
        }
    }

    public Optional<Resource> findById(Long id) {
        String sql = "SELECT * FROM resources WHERE id = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapResource(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            logger.error("Failed to find resource by id", e);
            throw new DataAccessException("Failed to find resource", e);
        }
    }

    public Optional<Resource> findByCode(String code) {
        String sql = "SELECT * FROM resources WHERE code = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, code);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapResource(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            logger.error("Failed to find resource by code", e);
            throw new DataAccessException("Failed to find resource", e);
        }
    }

    public List<Resource> findAll() {
        String sql = "SELECT * FROM resources ORDER BY created_at DESC";
        List<Resource> resources = new ArrayList<>();
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                resources.add(mapResource(rs));
            }
            return resources;
        } catch (SQLException e) {
            logger.error("Failed to list resources", e);
            throw new DataAccessException("Failed to list resources", e);
        }
    }

    private Resource mapResource(ResultSet rs) throws SQLException {
        Resource resource = new Resource();
        resource.setId(rs.getLong("id"));
        resource.setCode(rs.getString("code"));
        resource.setName(rs.getString("name"));
        resource.setType(rs.getString("type"));
        resource.setUrl(rs.getString("url"));
        resource.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return resource;
    }
}

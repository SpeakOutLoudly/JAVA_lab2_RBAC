package com.study.repository;

import com.study.domain.AuditLog;
import com.study.exception.DataAccessException;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AuditLogRepository extends BaseRepository {
    
    public AuditLogRepository(DatabaseConnection dbConnection) {
        super(dbConnection);
    }
    
    public void save(AuditLog auditLog) {
        String sql = """
            INSERT INTO audit_logs 
            (user_id, username, action, resource_type, resource_id, detail, success, error_message, ip_address)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            if (auditLog.getUserId() != null) {
                pstmt.setLong(1, auditLog.getUserId());
            } else {
                pstmt.setNull(1, Types.BIGINT);
            }
            pstmt.setString(2, auditLog.getUsername());
            pstmt.setString(3, auditLog.getAction());
            pstmt.setString(4, auditLog.getResourceType());
            pstmt.setString(5, auditLog.getResourceId());
            pstmt.setString(6, auditLog.getDetail());
            pstmt.setBoolean(7, auditLog.isSuccess());
            pstmt.setString(8, auditLog.getErrorMessage());
            pstmt.setString(9, auditLog.getIpAddress());
            
            pstmt.executeUpdate();
        } catch (SQLException e) {
            // Don't throw exception for audit log failure - just log it
            logger.error("Failed to save audit log", e);
        }
    }
    
    public List<AuditLog> findByUserId(Long userId, int limit) {
        String sql = """
            SELECT * FROM audit_logs 
            WHERE user_id = ? 
            ORDER BY created_at DESC 
            LIMIT ?
        """;
        List<AuditLog> logs = new ArrayList<>();
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, userId);
            pstmt.setInt(2, limit);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                logs.add(mapResultSetToAuditLog(rs));
            }
            return logs;
        } catch (SQLException e) {
            logger.error("Failed to find audit logs", e);
            throw new DataAccessException("Failed to find audit logs", e);
        }
    }
    
    public List<AuditLog> findAll(int limit) {
        String sql = "SELECT * FROM audit_logs ORDER BY created_at DESC LIMIT ?";
        List<AuditLog> logs = new ArrayList<>();
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, limit);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                logs.add(mapResultSetToAuditLog(rs));
            }
            return logs;
        } catch (SQLException e) {
            logger.error("Failed to find audit logs", e);
            throw new DataAccessException("Failed to find audit logs", e);
        }
    }
    
    public List<AuditLog> findByAction(String action, int limit) {
        String sql = "SELECT * FROM audit_logs WHERE action = ? ORDER BY created_at DESC LIMIT ?";
        List<AuditLog> logs = new ArrayList<>();
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, action);
            pstmt.setInt(2, limit);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                logs.add(mapResultSetToAuditLog(rs));
            }
            return logs;
        } catch (SQLException e) {
            logger.error("Failed to find audit logs by action", e);
            throw new DataAccessException("Failed to find audit logs", e);
        }
    }

    public List<AuditLog> findByResource(String resourceType, String resourceId, int limit) {
        String sql = resourceId == null || resourceId.isBlank()
                ? """
                    SELECT * FROM audit_logs
                    WHERE resource_type = ? AND resource_id IS NULL
                    ORDER BY created_at DESC
                    LIMIT ?
                  """
                : """
                    SELECT * FROM audit_logs
                    WHERE resource_type = ? AND resource_id = ?
                    ORDER BY created_at DESC
                    LIMIT ?
                  """;
        List<AuditLog> logs = new ArrayList<>();

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, resourceType);
            if (resourceId == null || resourceId.isBlank()) {
                pstmt.setInt(2, limit);
            } else {
                pstmt.setString(2, resourceId);
                pstmt.setInt(3, limit);
            }

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                logs.add(mapResultSetToAuditLog(rs));
            }
            return logs;
        } catch (SQLException e) {
            logger.error("Failed to find audit logs by resource", e);
            throw new DataAccessException("Failed to find audit logs", e);
        }
    }
    
    private AuditLog mapResultSetToAuditLog(ResultSet rs) throws SQLException {
        AuditLog log = new AuditLog();
        log.setId(rs.getLong("id"));
        long userId = rs.getLong("user_id");
        if (!rs.wasNull()) {
            log.setUserId(userId);
        }
        log.setUsername(rs.getString("username"));
        log.setAction(rs.getString("action"));
        log.setResourceType(rs.getString("resource_type"));
        log.setResourceId(rs.getString("resource_id"));
        log.setDetail(rs.getString("detail"));
        log.setSuccess(rs.getBoolean("success"));
        log.setErrorMessage(rs.getString("error_message"));
        log.setIpAddress(rs.getString("ip_address"));
        log.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return log;
    }
}

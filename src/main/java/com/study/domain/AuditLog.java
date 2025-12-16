package com.study.domain;

import java.time.LocalDateTime;

public class AuditLog {
    private Long id;
    private Long userId;
    private String username;
    private String action;
    private String resourceType;
    private String resourceId;
    private String detail;
    private boolean success;
    private String errorMessage;
    private String ipAddress;
    private LocalDateTime createdAt;

    public AuditLog() {
    }

    public AuditLog(Long userId, String username, String action, String resourceType, 
                    String resourceId, String detail, boolean success, String errorMessage) {
        this.userId = userId;
        this.username = username;
        this.action = action;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.detail = detail;
        this.success = success;
        this.errorMessage = errorMessage;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "AuditLog{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", action='" + action + '\'' +
                ", resourceType='" + resourceType + '\'' +
                ", success=" + success +
                ", createdAt=" + createdAt +
                '}';
    }
}

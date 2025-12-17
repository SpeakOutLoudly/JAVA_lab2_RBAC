package com.study.domain;

import java.util.Objects;

/**
 * Represents a permission grant that is limited to a specific resource.
 */
public class ScopedPermission {
    private Long roleId;
    private String permissionCode;
    private String resourceType;
    private String resourceId; // null means all resources of the given type

    public ScopedPermission() {}

    public ScopedPermission(Long roleId, String permissionCode, String resourceType, String resourceId) {
        this.roleId = roleId;
        this.permissionCode = permissionCode;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public String getPermissionCode() {
        return permissionCode;
    }

    public void setPermissionCode(String permissionCode) {
        this.permissionCode = permissionCode;
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

    public boolean matches(String targetType, String targetId) {
        if (targetType == null || !targetType.equalsIgnoreCase(resourceType)) {
            return false;
        }
        if (resourceId == null || resourceId.isBlank()) {
            return true;
        }
        return resourceId.equalsIgnoreCase(targetId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScopedPermission that = (ScopedPermission) o;
        return Objects.equals(permissionCode, that.permissionCode) &&
                Objects.equals(resourceType, that.resourceType) &&
                Objects.equals(resourceId, that.resourceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(permissionCode, resourceType, resourceId);
    }
}

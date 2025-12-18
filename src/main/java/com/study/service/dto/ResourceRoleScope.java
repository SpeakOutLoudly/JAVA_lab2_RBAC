package com.study.service.dto;

/**
 * DTO representing which role has a scoped permission on a resource.
 */
public class ResourceRoleScope {
    private String roleCode;
    private String permissionCode;
    private String scopeKey;

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }

    public String getPermissionCode() {
        return permissionCode;
    }

    public void setPermissionCode(String permissionCode) {
        this.permissionCode = permissionCode;
    }

    public String getScopeKey() {
        return scopeKey;
    }

    public void setScopeKey(String scopeKey) {
        this.scopeKey = scopeKey;
    }
}

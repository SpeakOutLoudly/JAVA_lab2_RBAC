package com.study.context;

import com.study.domain.Permission;
import com.study.domain.ScopedPermission;
import com.study.domain.User;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Session context holding current user information and cached permissions
 */
public class SessionContext {
    private User currentUser;
    private final Set<String> globalPermissions;
    private final List<ScopedPermission> scopedPermissions;
    
    public SessionContext() {
        this.globalPermissions = new HashSet<>();
        this.scopedPermissions = new ArrayList<>();
    }
    
    public User getCurrentUser() {
        return currentUser;
    }
    
    public void setCurrentUser(User user) {
        this.currentUser = user;
    }
    
    public Set<String> getEffectivePermissions() {
        return globalPermissions;
    }
    
    public void setPermissions(List<Permission> permissions, List<ScopedPermission> scoped) {
        this.globalPermissions.clear();
        for (Permission permission : permissions) {
            this.globalPermissions.add(permission.getCode());
        }
        this.scopedPermissions.clear();
        if (scoped != null) {
            this.scopedPermissions.addAll(scoped);
        }
    }
    
    public void refreshPermissions(List<Permission> permissions, List<ScopedPermission> scoped) {
        setPermissions(permissions, scoped);
    }
    
    public boolean hasPermission(String permissionCode) {
        return globalPermissions.contains(permissionCode);
    }
    
    public boolean hasPermission(String permissionCode, String resourceType, String resourceId) {
        if (permissionCode == null) {
            return true;
        }
        if (globalPermissions.contains(permissionCode)) {
            return true;
        }
        if (resourceType == null) {
            return false;
        }
        return scopedPermissions.stream()
                .filter(scope -> permissionCode.equals(scope.getPermissionCode()))
                .anyMatch(scope -> scope.matches(resourceType, resourceId));
    }
    
    public boolean isLoggedIn() {
        return currentUser != null;
    }
    
    public void clear() {
        this.currentUser = null;
        this.globalPermissions.clear();
        this.scopedPermissions.clear();
    }
}

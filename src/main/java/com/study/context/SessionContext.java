package com.study.context;

import com.study.domain.Permission;
import com.study.domain.User;

import java.util.HashSet;
import java.util.Set;

/**
 * Session context holding current user information and cached permissions
 */
public class SessionContext {
    private User currentUser;
    private Set<String> effectivePermissions;
    
    public SessionContext() {
        this.effectivePermissions = new HashSet<>();
    }
    
    public User getCurrentUser() {
        return currentUser;
    }
    
    public void setCurrentUser(User user) {
        this.currentUser = user;
    }
    
    public Set<String> getEffectivePermissions() {
        return effectivePermissions;
    }
    
    public void setEffectivePermissions(Set<Permission> permissions) {
        this.effectivePermissions.clear();
        for (Permission permission : permissions) {
            this.effectivePermissions.add(permission.getCode());
        }
    }
    
    public void refreshPermissions(Set<Permission> permissions) {
        setEffectivePermissions(permissions);
    }
    
    public boolean hasPermission(String permissionCode) {
        return effectivePermissions.contains(permissionCode);
    }
    
    public boolean isLoggedIn() {
        return currentUser != null;
    }
    
    public void clear() {
        this.currentUser = null;
        this.effectivePermissions.clear();
    }
}

package com.study.config;

/**
 * Command specification - maps commands to required permissions
 */
public enum CommandSpec {
    // Guest commands (no permission required)
    LOGIN("login", "User login", null),
    EXIT("exit", "Exit system", null),
    
    // User management commands
    CREATE_USER("create-user", "Create new user", PermissionCodes.USER_CREATE),
    LIST_USERS("list-users", "List all users", PermissionCodes.USER_LIST),
    VIEW_USER("view-user", "View user details", PermissionCodes.USER_VIEW),
    UPDATE_USER("update-user", "Update user", PermissionCodes.USER_UPDATE),
    DELETE_USER("delete-user", "Delete user", PermissionCodes.USER_DELETE),
    
    // Role management commands
    CREATE_ROLE("create-role", "Create new role", PermissionCodes.ROLE_CREATE),
    LIST_ROLES("list-roles", "List all roles", PermissionCodes.ROLE_VIEW),
    ASSIGN_ROLE("assign-role", "Assign role to user", PermissionCodes.ROLE_ASSIGN),
    REMOVE_ROLE("remove-role", "Remove role from user", PermissionCodes.ROLE_ASSIGN),
    
    // Permission management commands
    CREATE_PERMISSION("create-permission", "Create new permission", PermissionCodes.PERMISSION_CREATE),
    LIST_PERMISSIONS("list-permissions", "List all permissions", PermissionCodes.PERMISSION_VIEW),
    ASSIGN_PERMISSION("assign-permission", "Assign permission to role", PermissionCodes.PERMISSION_ASSIGN),
    REMOVE_PERMISSION("remove-permission", "Remove permission from role", PermissionCodes.PERMISSION_ASSIGN),
    
    // Audit commands
    VIEW_AUDIT_LOGS("view-audit", "View audit logs", PermissionCodes.AUDIT_VIEW),
    VIEW_ALL_AUDIT_LOGS("view-all-audit", "View all audit logs", PermissionCodes.AUDIT_VIEW_ALL),
    
    // User self-service
    CHANGE_PASSWORD("change-password", "Change own password", null),
    VIEW_PROFILE("view-profile", "View own profile", null),
    LOGOUT("logout", "Logout", null);
    
    private final String command;
    private final String description;
    private final String requiredPermission;
    
    CommandSpec(String command, String description, String requiredPermission) {
        this.command = command;
        this.description = description;
        this.requiredPermission = requiredPermission;
    }
    
    public String getCommand() {
        return command;
    }
    
    public String getDescription() {
        return description;
    }
    
    public String getRequiredPermission() {
        return requiredPermission;
    }
    
    public boolean requiresPermission() {
        return requiredPermission != null;
    }
    
    public static CommandSpec fromCommand(String command) {
        for (CommandSpec spec : values()) {
            if (spec.command.equals(command)) {
                return spec;
            }
        }
        return null;
    }
}

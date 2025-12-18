package com.study.config;

/**
 * Command specification - maps commands to required permissions.
 */
public enum CommandSpec {
    // Guest commands (no permission required)
    LOGIN("login", "User login", null),
    EXIT("exit", "Exit application", null),

    // User management commands
    CREATE_USER("create-user", "Create user", PermissionCodes.USER_CREATE),
    LIST_USERS("list-users", "List users", PermissionCodes.USER_LIST),
    VIEW_USER("view-user", "View user detail", PermissionCodes.USER_VIEW),
    UPDATE_USER("update-user", "Update or enable/disable user", PermissionCodes.USER_UPDATE),
    DELETE_USER("delete-user", "Delete user", PermissionCodes.USER_DELETE),

    // Role management commands
    CREATE_ROLE("create-role", "Create role", PermissionCodes.ROLE_CREATE),
    LIST_ROLES("list-roles", "List roles", PermissionCodes.ROLE_VIEW),
    ASSIGN_ROLE("assign-role", "Assign role to user", PermissionCodes.ROLE_ASSIGN),
    REMOVE_ROLE("remove-role", "Remove role from user", PermissionCodes.ROLE_ASSIGN),
    UPDATE_ROLE("update-role", "Update role", PermissionCodes.ROLE_UPDATE),
    DELETE_ROLE("delete-role", "Delete role", PermissionCodes.ROLE_DELETE),

    // Permission management commands
    CREATE_PERMISSION("create-permission", "Create permission", PermissionCodes.PERMISSION_CREATE),
    LIST_PERMISSIONS("list-permissions", "List permissions", PermissionCodes.PERMISSION_VIEW),
    LIST_MY_PERMISSIONS("list-my-permissions", "List my permissions", null),
    ASSIGN_PERMISSION("assign-permission", "Assign permission to role", PermissionCodes.PERMISSION_ASSIGN),
    REMOVE_PERMISSION("remove-permission", "Remove permission from role", PermissionCodes.PERMISSION_ASSIGN),
    UPDATE_PERMISSION("update-permission", "Update permission", PermissionCodes.PERMISSION_UPDATE),
    DELETE_PERMISSION("delete-permission", "Delete permission", PermissionCodes.PERMISSION_DELETE),
    ASSIGN_RESOURCE_PERMISSION("assign-resource-permission", "Grant scoped permission to role", PermissionCodes.RESOURCE_GRANT),
    REMOVE_RESOURCE_PERMISSION("remove-resource-permission", "Revoke scoped permission", PermissionCodes.RESOURCE_GRANT),

    // Resource commands
    CREATE_RESOURCE("create-resource", "Create resource", PermissionCodes.RESOURCE_CREATE),
    LIST_RESOURCES("list-resources", "List resources", PermissionCodes.RESOURCE_LIST),
    LIST_MY_RESOURCES("list-my-resources", "List my resources", null),
    VIEW_RESOURCE("view-resource", "View resource detail", PermissionCodes.RESOURCE_VIEW),
    UPDATE_RESOURCE("update-resource", "Update resource", PermissionCodes.RESOURCE_UPDATE),
    DELETE_RESOURCE("delete-resource", "Delete resource", PermissionCodes.RESOURCE_DELETE),

    // Audit commands
    VIEW_AUDIT_LOGS("view-audit", "View my audit logs", PermissionCodes.AUDIT_VIEW),
    VIEW_ALL_AUDIT_LOGS("view-all-audit", "View all audit logs", PermissionCodes.AUDIT_VIEW_ALL),
    VIEW_USER_AUDIT_LOGS("view-user-audit", "View audit logs by user", PermissionCodes.AUDIT_VIEW_ALL),
    VIEW_ACTION_AUDIT_LOGS("view-action-audit", "View audit logs by action", PermissionCodes.AUDIT_VIEW_ALL),
    VIEW_RESOURCE_AUDIT_LOGS("view-resource-audit", "View audit logs by resource", PermissionCodes.AUDIT_VIEW_ALL),

    // User self-service
    CHANGE_PASSWORD("change-password", "Change my password", null),
    VIEW_PROFILE("view-profile", "View my profile", null),
    CHANGE_PROFILE("change-profile", "Change my profile info", PermissionCodes.CHANGE_PROFILE),
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

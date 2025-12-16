package com.study.config;

/**
 * Command specification - maps commands to required permissions
 */
public enum CommandSpec {
    // Guest commands (no permission required)
    LOGIN("login", "用户登录", null),
    EXIT("exit", "退出系统", null),
    
    // User management commands
    CREATE_USER("create-user", "创建新用户", PermissionCodes.USER_CREATE),
    LIST_USERS("list-users", "查看用户列表", PermissionCodes.USER_LIST),
    VIEW_USER("view-user", "查看用户详情", PermissionCodes.USER_VIEW),
    UPDATE_USER("update-user", "更新用户", PermissionCodes.USER_UPDATE),
    DELETE_USER("delete-user", "删除用户", PermissionCodes.USER_DELETE),
    
    // Role management commands
    CREATE_ROLE("create-role", "创建新角色", PermissionCodes.ROLE_CREATE),
    LIST_ROLES("list-roles", "查看角色列表", PermissionCodes.ROLE_VIEW),
    ASSIGN_ROLE("assign-role", "分配角色给用户", PermissionCodes.ROLE_ASSIGN),
    REMOVE_ROLE("remove-role", "移除用户角色", PermissionCodes.ROLE_ASSIGN),
    
    // Permission management commands
    CREATE_PERMISSION("create-permission", "创建新权限", PermissionCodes.PERMISSION_CREATE),
    LIST_PERMISSIONS("list-permissions", "查看权限列表", PermissionCodes.PERMISSION_VIEW),
    ASSIGN_PERMISSION("assign-permission", "分配权限给角色", PermissionCodes.PERMISSION_ASSIGN),
    REMOVE_PERMISSION("remove-permission", "移除角色权限", PermissionCodes.PERMISSION_ASSIGN),
    
    // Audit commands
    VIEW_AUDIT_LOGS("view-audit", "查看审计日志", PermissionCodes.AUDIT_VIEW),
    VIEW_ALL_AUDIT_LOGS("view-all-audit", "查看所有审计日志", PermissionCodes.AUDIT_VIEW_ALL),
    
    // User self-service
    CHANGE_PASSWORD("change-password", "修改密码", null),
    VIEW_PROFILE("view-profile", "查看个人资料", null),
    LOGOUT("logout", "登出", null);
    
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

package com.study.config;

/**
 * Permission codes used in the system
 */
public class PermissionCodes {
    // User management
    public static final String USER_CREATE = "USER_CREATE";
    public static final String USER_UPDATE = "USER_UPDATE";
    public static final String USER_DELETE = "USER_DELETE";
    public static final String USER_VIEW = "USER_VIEW";
    public static final String USER_LIST = "USER_LIST";
    public static final String CHANGE_PROFILE = "CHANGE_PROFILE";
    
    // Role management
    public static final String ROLE_CREATE = "ROLE_CREATE";
    public static final String ROLE_UPDATE = "ROLE_UPDATE";
    public static final String ROLE_DELETE = "ROLE_DELETE";
    public static final String ROLE_VIEW = "ROLE_VIEW";
    public static final String ROLE_ASSIGN = "ROLE_ASSIGN";
    
    // Permission management
    public static final String PERMISSION_CREATE = "PERMISSION_CREATE";
    public static final String PERMISSION_UPDATE = "PERMISSION_UPDATE";
    public static final String PERMISSION_DELETE = "PERMISSION_DELETE";
    public static final String PERMISSION_VIEW = "PERMISSION_VIEW";
    public static final String PERMISSION_ASSIGN = "PERMISSION_ASSIGN";
    
    // Resource management
    public static final String RESOURCE_CREATE = "RESOURCE_CREATE";
    public static final String RESOURCE_UPDATE = "RESOURCE_UPDATE";
    public static final String RESOURCE_DELETE = "RESOURCE_DELETE";
    public static final String RESOURCE_VIEW = "RESOURCE_VIEW";
    public static final String RESOURCE_LIST = "RESOURCE_LIST";
    public static final String RESOURCE_GRANT = "RESOURCE_GRANT";
    
    // Audit management
    public static final String AUDIT_VIEW = "AUDIT_VIEW";
    public static final String AUDIT_VIEW_ALL = "AUDIT_VIEW_ALL";
    
    private PermissionCodes() {}
}

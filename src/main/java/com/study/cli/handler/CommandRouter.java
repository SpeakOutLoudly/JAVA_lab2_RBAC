package com.study.cli.handler;

import com.study.common.util.InputUtils;
import com.study.config.CommandSpec;
import com.study.domain.AuditLog;
import com.study.domain.Permission;
import com.study.domain.Resource;
import com.study.service.dto.ResourceAccessView;
import com.study.service.dto.ResourceRoleScope;
import com.study.service.dto.ResourceUserScope;
import com.study.domain.Role;
import com.study.domain.User;
import com.study.facade.RbacFacade;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Simple command router backed by a command map.
 */
public class CommandRouter {
    private static final int DEFAULT_LIMIT = 50;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Map<String, Command> commands = new LinkedHashMap<>();

    public CommandRouter() {
        register("help", "Show available commands", false, false, this::handleHelp);
        register("exit", "Exit application", false, true, facade -> System.out.println("Bye."));

        register("login", "Login", false, false, this::handleLogin);
        register("logout", "Logout", true, false, this::handleLogout);
        register("change-password", "Change my password", true, false, this::handleChangePassword);
        register("view-profile", "View my profile", true, false, this::handleViewProfile);
        register("change-profile", "Update my profile info", true, false, this::handleChangeProfile);

        register("create-user", "Create user", true, false, this::handleCreateUser);
        register("list-users", "List users", true, false, this::handleListUsers);
        register("view-user", "View user detail", true, false, this::handleViewUser);
        register("delete-user", "Delete user", true, false, this::handleDeleteUser);
        register("update-user", "Update/enable/disable user", true, false, this::handleUpdateUser);
        register("assign-role", "Assign role to user", true, false, this::handleAssignRole);
        register("remove-role", "Remove role from user", true, false, this::handleRemoveRole);

        register("create-role", "Create role", true, false, this::handleCreateRole);
        register("list-roles", "List roles", true, false, this::handleListRoles);
        register("update-role", "Update role", true, false, this::handleUpdateRole);
        register("delete-role", "Delete role", true, false, this::handleDeleteRole);

        register("create-permission", "Create permission", true, false, this::handleCreatePermission);
        register("list-permissions", "List permissions", true, false, this::handleListPermissions);
        register("list-my-permissions", "List my permissions", true, false, this::handleListMyPermissions);
        register("assign-permission", "Assign permission to role", true, false, this::handleAssignPermission);
        register("remove-permission", "Remove permission from role", true, false, this::handleRemovePermission);
        register("update-permission", "Update permission", true, false, this::handleUpdatePermission);
        register("delete-permission", "Delete permission", true, false, this::handleDeletePermission);
        register("assign-resource-permission", "Grant scoped permission to role", true, false, this::handleAssignScopedPermission);
        register("remove-resource-permission", "Revoke scoped permission from role", true, false, this::handleRemoveScopedPermission);

        register("create-resource", "Create resource", true, false, this::handleCreateResource);
        register("list-resources", "List resources", true, false, this::handleListResources);
        register("list-my-resources", "List my resources", true, false, this::handleListMyResources);
        register("view-resource", "View resource detail", true, false, this::handleViewResource);
        register("update-resource", "Update resource", true, false, this::handleUpdateResource);
        register("delete-resource", "Delete resource", true, false, this::handleDeleteResource);

        register("view-audit", "View my audit logs", true, false, this::handleViewAudit);
        register("view-all-audit", "View all audit logs", true, false, this::handleViewAllAudit);
        register("view-user-audit", "View audit logs by user", true, false, this::handleViewUserAudit);
        register("view-action-audit", "View audit logs by action", true, false, this::handleViewActionAudit);
        register("view-resource-audit", "View audit logs by resource", true, false, this::handleViewResourceAudit);
    }

    public boolean handle(String input, RbacFacade facade) {
        String commandKey = input.trim().toLowerCase();
        Command command = commands.get(commandKey);
        if (command == null) {
            System.out.println("Unknown command. Type 'help' to list available commands.");
            return true;
        }

        if (command.requiresLogin && !facade.isLoggedIn()) {
            System.out.println("Please login first.");
            return true;
        }

        CommandSpec spec = CommandSpec.fromCommand(commandKey);
        if (spec != null && !facade.canExecuteCommand(commandKey)) {
            String requiredPerm = spec.getRequiredPermission() != null
                    ? spec.getRequiredPermission()
                    : "None";
            System.out.println("Permission denied: " + commandKey + " (required: " + requiredPerm + ")");
            return true;
        }

        command.action.accept(facade);
        return !command.exits;
    }

    private void register(String name, String description, boolean requiresLogin,
                          boolean exits, Consumer<RbacFacade> action) {
        commands.put(name, new Command(name, description, requiresLogin, exits, action));
    }

    private void handleHelp(RbacFacade facade) {
        boolean loggedIn = facade.isLoggedIn();
        System.out.println("\n== Commands ==");

        commands.values().forEach(cmd -> {
            if (cmd.requiresLogin && !loggedIn) {
                return;
            }
            CommandSpec spec = CommandSpec.fromCommand(cmd.name);
            if (spec != null && !facade.canExecuteCommand(cmd.name)) {
                return;
            }
            System.out.printf("%-28s : %s%n", cmd.name, cmd.description);
        });
    }

    // ---- Auth ----

    private void handleLogin(RbacFacade facade) {
        String username = InputUtils.readInput("Username: ");
        String password = InputUtils.readPassword("Password: ");
        User user = facade.login(username, password);
        System.out.println("[SUCCESS] Logged in as " + user.getUsername());
    }

    private void handleLogout(RbacFacade facade) {
        facade.logout();
        System.out.println("[SUCCESS] Logged out.");
    }

    private void handleChangePassword(RbacFacade facade) {
        String oldPassword = InputUtils.readPassword("Old password: ");
        String newPassword = InputUtils.readPassword("New password: ");
        facade.changePassword(oldPassword, newPassword);
        System.out.println("[SUCCESS] Password changed.");
    }

    private void handleViewProfile(RbacFacade facade) {
        User user = facade.getCurrentUser();
        System.out.println("\n== Profile ==");
        System.out.println("ID: " + user.getId());
        System.out.println("Username: " + user.getUsername());
        System.out.println("Real Name: " + orDash(user.getRealName()));
        System.out.println("Email: " + orDash(user.getEmail()));
        System.out.println("Phone: " + orDash(user.getPhone()));
        System.out.println("Status: " + (user.isEnabled() ? "ENABLED" : "DISABLED"));
    }

    private void handleChangeProfile(RbacFacade facade) {
        System.out.println("\n== Update Profile ==");
        String email = InputUtils.readEmail("Email (leave blank to skip): ");
        String phone = InputUtils.readPhone("Phone (leave blank to skip): ");
        String realName = InputUtils.readInput("Real Name (leave blank to skip): ");
        facade.updateUserInfo(email.isBlank() ? null : email,
                phone.isBlank() ? null : phone,
                realName.isBlank() ? null : realName);
        System.out.println("[SUCCESS] Profile updated.");
    }

    // ---- User ----

    private void handleCreateUser(RbacFacade facade) {
        String username = InputUtils.readInput("Username: ");
        String password = InputUtils.readPassword("Password: ");
        String email = InputUtils.readEmail("Email (optional): ");
        String phone = InputUtils.readPhone("Phone (optional): ");
        String realName = InputUtils.readInput("Real Name (optional): ");
        String roleCode = InputUtils.readInput("Assign role (optional): ");

        User user;
        if (roleCode.isBlank()) {
            user = facade.createUser(username, password,
                    email.isBlank() ? null : email,
                    phone.isBlank() ? null : phone,
                    realName.isBlank() ? null : realName);
        } else {
            user = facade.createUserWithRole(username, password, roleCode,
                    email.isBlank() ? null : email,
                    phone.isBlank() ? null : phone,
                    realName.isBlank() ? null : realName);
        }

        System.out.println("[SUCCESS] User created: " + user.getUsername());
    }

    private void handleListUsers(RbacFacade facade) {
        List<User> users = facade.listUsers();
        System.out.println("\n== Users (" + users.size() + ") ==");
        System.out.printf("%-6s %-20s %-10s %s%n", "ID", "Username", "Status", "Roles");
        System.out.println("-".repeat(70));

        for (User user : users) {
            List<Role> roles = facade.getUserRoles(user.getUsername());
            String roleNames = roles.isEmpty()
                    ? "-"
                    : roles.stream().map(Role::getName).collect(Collectors.joining(", "));

            System.out.printf("%-6d %-20s %-10s %s%n",
                    user.getId(),
                    user.getUsername(),
                    user.isEnabled() ? "ENABLED" : "DISABLED",
                    roleNames);
        }
    }

    private void handleViewUser(RbacFacade facade) {
        String username = InputUtils.readInput("Username: ");
        User user = facade.viewUser(username);

        System.out.println("\n== User Detail ==");
        System.out.println("ID: " + user.getId());
        System.out.println("Username: " + user.getUsername());
        System.out.println("Real Name: " + orDash(user.getRealName()));
        System.out.println("Email: " + orDash(user.getEmail()));
        System.out.println("Phone: " + orDash(user.getPhone()));
        System.out.println("Status: " + (user.isEnabled() ? "ENABLED" : "DISABLED"));
        List<Role> roles = facade.getUserRoles(username);
        System.out.println("Roles: " + (roles.isEmpty()
                ? "-"
                : roles.stream().map(Role::getName).toList()));
    }

    private void handleDeleteUser(RbacFacade facade) {
        long userId = InputUtils.readLong("User ID: ");
        String confirm = InputUtils.readInput("Confirm delete user? (yes/no): ");
        if ("yes".equalsIgnoreCase(confirm)) {
            facade.deleteUser(userId);
            System.out.println("[SUCCESS] User deleted.");
        } else {
            System.out.println("Delete cancelled.");
        }
    }

    private void handleUpdateUser(RbacFacade facade) {
        long userId = InputUtils.readLong("User ID: ");
        System.out.println("\nChoose action:");
        System.out.println("1. Reset password");
        System.out.println("2. Enable user");
        System.out.println("3. Disable user");
        String choice = InputUtils.readInput("Select (1-3): ");

        switch (choice) {
            case "1" -> {
                String newPassword = InputUtils.readPassword("New password: ");
                facade.resetPassword(userId, newPassword);
                System.out.println("[SUCCESS] Password reset.");
            }
            case "2" -> {
                facade.enableUser(userId);
                System.out.println("[SUCCESS] User enabled.");
            }
            case "3" -> {
                facade.disableUser(userId);
                System.out.println("[SUCCESS] User disabled.");
            }
            default -> System.out.println("Invalid option.");
        }
    }

    private void handleAssignRole(RbacFacade facade) {
        String username = InputUtils.readInput("Username: ");
        String roleCode = InputUtils.readInput("Role code: ");

        facade.assignRoleToUser(username, roleCode);
        System.out.println("[SUCCESS] Role assigned to user.");
    }

    private void handleRemoveRole(RbacFacade facade) {
        String username = InputUtils.readInput("Username: ");
        String roleCode = InputUtils.readInput("Role code: ");

        facade.removeRoleFromUser(username, roleCode);
        System.out.println("[SUCCESS] Role removed from user.");
    }

    // ---- Role ----

    private void handleCreateRole(RbacFacade facade) {
        String code = InputUtils.readInput("Role code: ");
        String name = InputUtils.readInput("Role name: ");
        String description = InputUtils.readInput("Description (optional): ");

        Role role = facade.createRole(code, name, description.isBlank() ? null : description);
        System.out.println("[SUCCESS] Role created: " + role.getCode());
    }

    private void handleListRoles(RbacFacade facade) {
        List<Role> roles = facade.listRoles();
        System.out.println("\n== Roles (" + roles.size() + ") ==");
        roles.forEach(role -> System.out.printf("[%d] %s - %s%n", role.getId(), role.getCode(), role.getName()));
    }

    private void handleUpdateRole(RbacFacade facade) {
        long roleId = InputUtils.readLong("Role ID: ");
        String name = InputUtils.readInput("New name (blank to keep): ");
        String description = InputUtils.readInput("New description (blank to keep): ");

        Role role = facade.updateRole(roleId,
                name.isBlank() ? null : name,
                description.isBlank() ? null : description);
        System.out.println("[SUCCESS] Role updated: " + role.getCode());
    }

    private void handleDeleteRole(RbacFacade facade) {
        long roleId = InputUtils.readLong("Role ID: ");
        String confirm = InputUtils.readInput("Confirm delete? (yes/no): ");
        if ("yes".equalsIgnoreCase(confirm)) {
            facade.deleteRole(roleId);
            System.out.println("[SUCCESS] Role deleted.");
        } else {
            System.out.println("Delete cancelled.");
        }
    }

    // ---- Permission ----

    private void handleCreatePermission(RbacFacade facade) {
        String code = InputUtils.readInput("Permission code: ");
        String name = InputUtils.readInput("Permission name: ");
        String description = InputUtils.readInput("Description (optional): ");

        Permission permission = facade.createPermission(code, name, description.isBlank() ? null : description);
        System.out.println("[SUCCESS] Permission created: " + permission.getCode());
    }

    private void handleListPermissions(RbacFacade facade) {
        List<Permission> permissions = facade.listPermissions();
        System.out.println("\n== Permissions (" + permissions.size() + ") ==");
        permissions.forEach(p -> System.out.printf("[%d] %s - %s%n", p.getId(), p.getCode(), p.getName()));
    }

    private void handleListMyPermissions(RbacFacade facade) {
        List<Permission> permissions = facade.listMyPermissions();
        System.out.println("\n== My Permissions (" + permissions.size() + ") ==");
        if (permissions.isEmpty()) {
            System.out.println("No permissions assigned.");
            return;
        }
        permissions.forEach(p -> System.out.printf("[%d] %s - %s%n", p.getId(), p.getCode(), p.getName()));
    }

    private void handleUpdatePermission(RbacFacade facade) {
        String code = InputUtils.readInput("Permission code: ");
        String name = InputUtils.readInput("New name (blank to keep): ");
        String description = InputUtils.readInput("New description (blank to keep): ");
        String resourceIdStr = InputUtils.readInput("Bind resource ID (blank for none): ");
        Long resourceId = null;
        if (!resourceIdStr.isBlank()) {
            try {
                resourceId = Long.parseLong(resourceIdStr);
            } catch (NumberFormatException ex) {
                System.out.println("Invalid resource ID, update cancelled.");
                return;
            }
        }

        Permission permission = facade.updatePermission(code,
                name.isBlank() ? null : name,
                description.isBlank() ? null : description,
                resourceId);
        System.out.println("[SUCCESS] Permission updated: " + permission.getCode());
    }

    private void handleDeletePermission(RbacFacade facade) {
        String code = InputUtils.readInput("Permission code: ");
        String confirm = InputUtils.readInput("Confirm delete? (yes/no): ");
        if ("yes".equalsIgnoreCase(confirm)) {
            facade.deletePermission(code);
            System.out.println("[SUCCESS] Permission deleted");
        } else {
            System.out.println("Delete cancelled.");
        }
    }

    private void handleAssignPermission(RbacFacade facade) {
        String roleCode = InputUtils.readInput("Role code: ");
        String permissionCode = InputUtils.readInput("Permission code: ");

        facade.assignPermissionToRole(roleCode, permissionCode);
        System.out.println("[SUCCESS] Permission assigned.");
    }

    private void handleRemovePermission(RbacFacade facade) {
        String roleCode = InputUtils.readInput("Role code: ");
        String permissionCode = InputUtils.readInput("Permission code: ");

        facade.removePermissionFromRole(roleCode, permissionCode);
        System.out.println("[SUCCESS] Permission removed.");
    }

    private void handleAssignScopedPermission(RbacFacade facade) {
        String roleCode = InputUtils.readInput("Role code: ");
        String permissionCode = InputUtils.readInput("Permission code: ");
        String resourceType = InputUtils.readInput("Resource type: ");
        String resourceId = InputUtils.readInput("Resource ID (blank for all within type): ");

        facade.assignScopedPermission(roleCode, permissionCode, resourceType, resourceId.isBlank() ? null : resourceId);
        System.out.println("[SUCCESS] Scoped permission assigned.");
    }

    private void handleRemoveScopedPermission(RbacFacade facade) {
        String roleCode = InputUtils.readInput("Role code: ");
        String permissionCode = InputUtils.readInput("Permission code: ");
        String resourceType = InputUtils.readInput("Resource type: ");
        String resourceId = InputUtils.readInput("Resource ID (blank for all within type): ");

        facade.removeScopedPermission(roleCode, permissionCode, resourceType, resourceId.isBlank() ? null : resourceId);
        System.out.println("[SUCCESS] Scoped permission removed.");
    }

    // ---- Resource ----

    private void handleCreateResource(RbacFacade facade) {
        String code = InputUtils.readInput("Resource code: ");
        String name = InputUtils.readInput("Resource name: ");
        String type = InputUtils.readInput("Resource type: ");
        String url = InputUtils.readInput("Resource url (optional): ");

        Resource resource = facade.createResource(code, name, type, url.isBlank() ? null : url);
        System.out.println("[SUCCESS] Resource created: " + resource.getCode());
    }

    private void handleListResources(RbacFacade facade) {
        List<Resource> resources = facade.listResources();
        System.out.println("\n== Resources (" + resources.size() + ") ==");
        resources.forEach(r ->
                System.out.printf("[%d] %s (%s)%n", r.getId(), r.getCode(), r.getType())
        );
    }

    private void handleListMyResources(RbacFacade facade) {
        List<Resource> resources = facade.listMyResources();
        if (resources.isEmpty()) {
            System.out.println("\n== You don't have any scoped resources ==");
            return;
        }
        System.out.printf("\n== My Resources (%d) ==%n", resources.size());
        for (Resource r : resources) {
            System.out.printf("[%d] %s (%s)%n", r.getId(), r.getCode(), r.getType());
        }
    }

    private void handleViewResource(RbacFacade facade) {
        long resourceId = InputUtils.readLong("Resource ID: ");
        Resource r = facade.getResource(resourceId);
        ResourceAccessView access = facade.getResourceAccess(resourceId);

        System.out.println("\n== Resource Detail ==");
        System.out.printf("ID: %d%n", r.getId());
        System.out.printf("Code: %s%n", r.getCode());
        System.out.printf("Name: %s%n", r.getName());
        System.out.printf("Type: %s%n", r.getType());
        System.out.printf("URL: %s%n", (r.getUrl() != null ? r.getUrl() : "-"));

        System.out.println();
        System.out.println("-- Roles with scoped permissions --");
        if (access.getRoleScopes() == null || access.getRoleScopes().isEmpty()) {
            System.out.println("(no roles)");
        } else {
            for (ResourceRoleScope rs : access.getRoleScopes()) {
                System.out.printf("  Role=%s, Perm=%s, Scope=%s%n",
                        rs.getRoleCode(), rs.getPermissionCode(), rs.getScopeKey());
            }
        }

        System.out.println();
        System.out.println("-- Users with scoped permissions --");
        if (access.getUserScopes() == null || access.getUserScopes().isEmpty()) {
            System.out.println("(no users)");
        } else {
            for (ResourceUserScope us : access.getUserScopes()) {
                System.out.printf("  User=%s, Role=%s, Perm=%s%n",
                        us.getUsername(), us.getRoleCode(), us.getPermissionCode());
            }
        }
    }

    private void handleUpdateResource(RbacFacade facade) {
        long resourceId = InputUtils.readLong("Resource ID: ");
        String name = InputUtils.readInput("New name (leave blank to keep): ");
        String type = InputUtils.readInput("New type (leave blank to keep): ");
        String url = InputUtils.readInput("New url (leave blank to keep): ");

        Resource resource = facade.updateResource(resourceId,
                name.isBlank() ? null : name,
                type.isBlank() ? null : type,
                url.isBlank() ? null : url);
        System.out.println("[SUCCESS] Resource updated: " + resource.getCode());
    }

    private void handleDeleteResource(RbacFacade facade) {
        long resourceId = InputUtils.readLong("Resource ID: ");
        String confirm = InputUtils.readInput("Confirm delete? (yes/no): ");
        if ("yes".equalsIgnoreCase(confirm)) {
            facade.deleteResource(resourceId);
            System.out.println("[SUCCESS] Resource deleted");
        } else {
            System.out.println("Delete cancelled.");
        }
    }

    // ---- Audit ----

    private void handleViewAudit(RbacFacade facade) {
        displayLogs(facade.viewMyAuditLogs(readLimit()));
    }

    private void handleViewAllAudit(RbacFacade facade) {
        displayLogs(facade.viewAllAuditLogs(readLimit()));
    }

    private void handleViewUserAudit(RbacFacade facade) {
        long userId = InputUtils.readLong("User ID: ");
        displayLogs(facade.viewUserAuditLogs(userId, readLimit()));
    }

    private void handleViewActionAudit(RbacFacade facade) {
        String action = InputUtils.readInput("Action keyword: ");
        displayLogs(facade.viewAuditLogsByAction(action, readLimit()));
    }

    private void handleViewResourceAudit(RbacFacade facade) {
        String resourceType = InputUtils.readInput("Resource type: ");
        String resourceId = InputUtils.readInput("Resource ID (blank for all): ");
        displayLogs(facade.viewAuditLogsByResource(resourceType, resourceId.isBlank() ? null : resourceId, readLimit()));
    }

    private void displayLogs(List<AuditLog> logs) {
        System.out.println("\n== Audit Logs (" + logs.size() + ") ==");
        if (logs.isEmpty()) {
            System.out.println("No audit records found.");
            return;
        }
        logs.forEach(log -> {
            String timestamp = log.getCreatedAt().format(FORMATTER);
            String username = log.getUsername() != null ? log.getUsername() : "N/A";
            String result = log.isSuccess() ? "SUCCESS" : "FAILED";
            String detail = log.isSuccess() ? log.getDetail() : log.getErrorMessage();
            System.out.printf("%s | %s | %s | %s | %s | %s%n",
                    timestamp,
                    username,
                    log.getAction(),
                    log.getResourceType() != null ? log.getResourceType() : "-",
                    log.getResourceId() != null ? log.getResourceId() : "-",
                    result + (detail != null ? " (" + detail + ")" : ""));
        });
    }

    private int readLimit() {
        return InputUtils.readIntOrDefault("Number of records (default 50): ", DEFAULT_LIMIT);
    }

    private String orDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private record Command(String name, String description, boolean requiresLogin, boolean exits,
                           Consumer<RbacFacade> action) {
    }
}

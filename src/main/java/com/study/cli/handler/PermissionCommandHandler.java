package com.study.cli.handler;

import com.study.common.util.InputUtils;
import com.study.domain.Permission;
import com.study.domain.ScopedPermission;
import com.study.facade.RbacFacade;

import java.util.List;

/**
 * Handler for permission management commands.
 */
public class PermissionCommandHandler implements CommandHandler {

    @Override
    public void handle(String command, RbacFacade facade) {
        switch (command) {
            case "create-permission" -> handleCreatePermission(facade);
            case "list-permissions" -> handleListPermissions(facade);
            case "list-my-permissions" -> handleListMyPermissions(facade);
            case "update-permission" -> handleUpdatePermission(facade);
            case "delete-permission" -> handleDeletePermission(facade);
            case "assign-permission" -> handleAssignPermission(facade);
            case "remove-permission" -> handleRemovePermission(facade);
            case "assign-resource-permission" -> handleAssignScopedPermission(facade);
            case "remove-resource-permission" -> handleRemoveScopedPermission(facade);
            default -> System.out.println("Unknown permission command: " + command);
        }
    }

    private void handleCreatePermission(RbacFacade facade) {
        String code = InputUtils.readInput("Permission code: ");
        String name = InputUtils.readInput("Permission name: ");
        String description = InputUtils.readInput("Description (optional): ");

        Permission permission = facade.createPermission(code, name, description.isBlank() ? null : description);
        System.out.println("✔ Permission created: " + permission.getCode());
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
        System.out.println("✔ Permission updated: " + permission.getCode());
    }

    private void handleDeletePermission(RbacFacade facade) {
        String code = InputUtils.readInput("Permission code: ");
        String confirm = InputUtils.readInput("Confirm delete? (yes/no): ");
        if ("yes".equalsIgnoreCase(confirm)) {
            facade.deletePermission(code);
            System.out.println("✔ Permission deleted");
        } else {
            System.out.println("Delete cancelled.");
        }
    }

    private void handleAssignPermission(RbacFacade facade) {
        String roleCode = InputUtils.readInput("Role code: ");
        String permissionCode = InputUtils.readInput("Permission code: ");

        facade.assignPermissionToRole(roleCode, permissionCode);
        System.out.println("✔ Permission assigned.");
    }

    private void handleRemovePermission(RbacFacade facade) {
        String roleCode = InputUtils.readInput("Role code: ");
        String permissionCode = InputUtils.readInput("Permission code: ");

        facade.removePermissionFromRole(roleCode, permissionCode);
        System.out.println("✔ Permission removed.");
    }

    private void handleAssignScopedPermission(RbacFacade facade) {
        String roleCode = InputUtils.readInput("Role code: ");
        String permissionCode = InputUtils.readInput("Permission code: ");
        String resourceType = InputUtils.readInput("Resource type: ");
        String resourceId = InputUtils.readInput("Resource ID (blank for all within type): ");

        facade.assignScopedPermission(roleCode, permissionCode, resourceType, resourceId.isBlank() ? null : resourceId);
        System.out.println("✔ Scoped permission assigned.");
    }

    private void handleRemoveScopedPermission(RbacFacade facade) {
        String roleCode = InputUtils.readInput("Role code: ");
        String permissionCode = InputUtils.readInput("Permission code: ");
        String resourceType = InputUtils.readInput("Resource type: ");
        String resourceId = InputUtils.readInput("Resource ID (blank for all within type): ");

        facade.removeScopedPermission(roleCode, permissionCode, resourceType, resourceId.isBlank() ? null : resourceId);
        System.out.println("✔ Scoped permission removed.");
    }

    @SuppressWarnings("unused")
    private void displayScopedPermissions(RbacFacade facade, String roleCode) {
        List<ScopedPermission> scopedPermissions = facade.getScopedPermissionsForRole(roleCode);
        System.out.println("\n== Scoped permissions for " + roleCode + " ==");
        if (scopedPermissions.isEmpty()) {
            System.out.println("No scoped permissions.");
            return;
        }
        scopedPermissions.forEach(scope ->
                System.out.printf("%s -> %s (%s)%n",
                        scope.getPermissionCode(),
                        scope.getResourceType(),
                        scope.getResourceId() != null ? scope.getResourceId() : "ALL")
        );
    }
}

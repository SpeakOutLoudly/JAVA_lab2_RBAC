package com.study.cli.handler;

import com.study.common.util.InputUtils;
import com.study.domain.Role;
import com.study.facade.RbacFacade;

import java.util.List;

/**
 * Handler for role management commands.
 */
public class RoleCommandHandler implements CommandHandler {

    @Override
    public void handle(String command, RbacFacade facade) {
        switch (command) {
            case "create-role" -> handleCreateRole(facade);
            case "list-roles" -> handleListRoles(facade);
            case "assign-role" -> handleAssignRole(facade);
            case "remove-role" -> handleRemoveRole(facade);
            case "update-role" -> handleUpdateRole(facade);
            case "delete-role" -> handleDeleteRole(facade);
            default -> System.out.println("Unknown role command: " + command);
        }
    }

    private void handleCreateRole(RbacFacade facade) {
        String code = InputUtils.readInput("Role code: ");
        String name = InputUtils.readInput("Role name: ");
        String description = InputUtils.readInput("Description (optional): ");

        Role role = facade.createRole(code, name, description.isBlank() ? null : description);
        System.out.println("✔ Role created: " + role.getCode());
    }

    private void handleListRoles(RbacFacade facade) {
        List<Role> roles = facade.listRoles();
        System.out.println("\n== Roles (" + roles.size() + ") ==");
        roles.forEach(role -> System.out.printf("[%d] %s - %s%n", role.getId(), role.getCode(), role.getName()));
    }

    private void handleAssignRole(RbacFacade facade) {
        String username = InputUtils.readInput("Username: ");
        String roleCode = InputUtils.readInput("Role code: ");

        facade.assignRoleToUser(username, roleCode);
        System.out.println("✔ Role assigned to user.");
    }

    private void handleRemoveRole(RbacFacade facade) {
        String username = InputUtils.readInput("Username: ");
        String roleCode = InputUtils.readInput("Role code: ");

        facade.removeRoleFromUser(username, roleCode);
        System.out.println("✔ Role removed from user.");
    }

    private void handleUpdateRole(RbacFacade facade) {
        long roleId = InputUtils.readLong("Role ID: ");
        String name = InputUtils.readInput("New name (blank to keep): ");
        String description = InputUtils.readInput("New description (blank to keep): ");

        Role role = facade.updateRole(roleId,
                name.isBlank() ? null : name,
                description.isBlank() ? null : description);
        System.out.println("✔ Role updated: " + role.getCode());
    }

    private void handleDeleteRole(RbacFacade facade) {
        long roleId = InputUtils.readLong("Role ID: ");
        String confirm = InputUtils.readInput("Confirm delete? (yes/no): ");
        if ("yes".equalsIgnoreCase(confirm)) {
            facade.deleteRole(roleId);
            System.out.println("✔ Role deleted.");
        } else {
            System.out.println("Delete cancelled.");
        }
    }
}

package com.study.cli.handler;

import com.study.common.util.InputUtils;
import com.study.domain.Role;
import com.study.domain.User;
import com.study.facade.RbacFacade;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Handler for user management commands.
 */
public class UserCommandHandler implements CommandHandler {

    @Override
    public void handle(String command, RbacFacade facade) {
        switch (command) {
            case "create-user" -> handleCreateUser(facade);
            case "list-users" -> handleListUsers(facade);
            case "view-user" -> handleViewUser(facade);
            case "delete-user" -> handleDeleteUser(facade);
            case "update-user" -> handleUpdateUser(facade);
            default -> System.out.println("Unknown user command: " + command);
        }
    }

    private void handleCreateUser(RbacFacade facade) {
        String username = InputUtils.readInput("Username: ");
        String password = InputUtils.readPassword("Password: ");
        String roleCode = InputUtils.readInput("Assign role (optional): ");

        User user = roleCode.isBlank()
                ? facade.createUser(username, password)
                : facade.createUserWithRole(username, password, roleCode);

        System.out.println("✔ User created: " + user.getUsername());
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
        System.out.println("Status: " + (user.isEnabled() ? "ENABLED" : "DISABLED"));
        List<Role> roles = facade.getUserRoles(username);
        System.out.println("Roles: " + roles.stream().map(Role::getName).toList());
    }

    private void handleDeleteUser(RbacFacade facade) {
        long userId = InputUtils.readLong("User ID: ");
        String confirm = InputUtils.readInput("Confirm delete user? (yes/no): ");
        if ("yes".equalsIgnoreCase(confirm)) {
            facade.deleteUser(userId);
            System.out.println("✔ User deleted.");
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
                System.out.println("✔ Password reset.");
            }
            case "2" -> {
                facade.enableUser(userId);
                System.out.println("✔ User enabled.");
            }
            case "3" -> {
                facade.disableUser(userId);
                System.out.println("✔ User disabled.");
            }
            default -> System.out.println("Invalid option.");
        }
    }
}

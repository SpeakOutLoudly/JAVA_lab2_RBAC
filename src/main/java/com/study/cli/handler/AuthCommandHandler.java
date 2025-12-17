package com.study.cli.handler;

import com.study.common.util.InputUtils;
import com.study.domain.Permission;
import com.study.domain.Role;
import com.study.domain.User;
import com.study.facade.RbacFacade;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Handler for authentication related commands.
 */
public class AuthCommandHandler implements CommandHandler {

    @Override
    public void handle(String command, RbacFacade facade) {
        switch (command) {
            case "logout" -> handleLogout(facade);
            case "change-password" -> handleChangePassword(facade);
            case "view-profile" -> handleViewProfile(facade);
            default -> System.out.println("Unknown auth command: " + command);
        }
    }

    private void handleLogout(RbacFacade facade) {
        facade.logout();
        System.out.println("[SUCCESS] Logged out.");
    }

    private void handleChangePassword(RbacFacade facade) {
        String oldPassword = InputUtils.readPassword("Current password: ");
        String newPassword = InputUtils.readPassword("New password: ");
        String confirm = InputUtils.readPassword("Confirm new password: ");

        if (!newPassword.equals(confirm)) {
            System.out.println("⚠ Password mismatch.");
            return;
        }

        facade.changePassword(oldPassword, newPassword);
        System.out.println("[SUCCESS] Password updated.");
    }

    private void handleViewProfile(RbacFacade facade) {
        User user = facade.getCurrentUser();
        System.out.println("\n== Profile ==");
        System.out.println("ID: " + user.getId());
        System.out.println("Username: " + user.getUsername());
        System.out.println("Status: " + (user.isEnabled() ? "ENABLED" : "DISABLED"));
        System.out.println("Created at: " + user.getCreatedAt());

        List<Role> roles = facade.getUserRoles(user.getUsername());
        System.out.println("Roles: " + roles.stream().map(Role::getName).toList());

        List<Permission> permissions = facade.getUserPermissions(user.getUsername());
        String permPreview = permissions.stream()
                .map(Permission::getCode)
                .limit(5)
                .collect(Collectors.joining(", "));
        System.out.println("Permissions (" + permissions.size() + "): " + permPreview + (permissions.size() > 5 ? "..." : ""));
    }
}

package com.study.cli;

import com.study.cli.handler.CommandRouter;
import com.study.common.util.InputUtils;
import com.study.exception.RbacException;
import com.study.facade.RbacFacade;
import com.study.config.CommandSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Minimal CLI application loop with a map-based command router.
 */
public class CliApplication {
    private final RbacFacade facade;
    private final CommandRouter router;
    private final boolean debugEnabled = Boolean.getBoolean("cli.debug");

    public CliApplication(RbacFacade facade) {
        this.facade = facade;
        this.router = new CommandRouter();
    }

    /**
     * Start the CLI application.
     */
    public void start() {
        System.out.println("==============================");
        System.out.println("  RBAC CLI - Access Control  ");
        System.out.println("==============================");
        System.out.println("\nDefault admin: admin / admin123");
        System.out.println("Use numbers to navigate menus.\n");

        boolean running = true;
        while (running) {
            try {
                if (!facade.isLoggedIn()) {
                    running = handleLoginMenu();
                } else {
                    running = handleMainMenu();
                }
            } catch (RbacException e) {
                System.out.println("\n[ERROR] " + e.getMessage());
            } catch (Exception e) {
                System.out.println("\n[ERROR] Internal error: " + e.getMessage());
                if (debugEnabled) {
                    e.printStackTrace();
                }
            }
            System.out.println();
        }

        InputUtils.close();
    }

    private boolean handleLoginMenu() {
        System.out.println("====== Login Menu ======");
        System.out.println("1. Login");
        System.out.println("0. Exit");
        String input = InputUtils.readInput("guest> ").trim();
        switch (input) {
            case "1" -> router.handle("login", facade);
            case "0" -> {
                System.out.println("Bye.");
                return false;
            }
            default -> System.out.println("Invalid choice. Please select 0 or 1.");
        }
        return true;
    }

    private boolean handleMainMenu() {
        List<MenuCategory> categories = buildCategories();
        boolean inMain = true;
        while (inMain) {
            printMainMenu(categories);
            String input = InputUtils.readInput(facade.getCurrentUser().getUsername() + "> ").trim();
            if (input.isEmpty()) {
                continue;
            }
            if ("0".equals(input)) {
                router.handle("logout", facade);
                return true;
            }
            int idx;
            try {
                idx = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Invalid choice. Enter a number.");
                continue;
            }
            if (idx < 1 || idx > categories.size()) {
                System.out.println("Invalid choice.");
                continue;
            }
            MenuCategory category = categories.get(idx - 1);
            handleSubMenu(category);
        }
        return true;
    }

    private void handleSubMenu(MenuCategory category) {
        while (true) {
            List<String> commands = category.commands.stream()
                    .filter(cmd -> facade.canExecuteCommand(cmd))
                    .toList();
            if (commands.isEmpty()) {
                System.out.println("No commands available in this menu (insufficient permissions).");
                return;
            }
            System.out.println("\n-- " + category.label + " --");
            for (int i = 0; i < commands.size(); i++) {
                String cmd = commands.get(i);
                String desc = describe(cmd);
                System.out.printf("%d. %s%s%n", i + 1, cmd, desc != null ? " - " + desc : "");
            }
            System.out.println("0. Back");
            String input = InputUtils.readInput("menu> ").trim();
            if ("0".equals(input)) {
                return;
            }
            int idx;
            try {
                idx = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Invalid choice. Enter a number.");
                continue;
            }
            if (idx < 1 || idx > commands.size()) {
                System.out.println("Invalid choice.");
                continue;
            }
            String command = commands.get(idx - 1);
            router.handle(command, facade);
            // return to main menu after executing one command
            return;
        }
    }

    private void printMainMenu(List<MenuCategory> categories) {
        System.out.println("\n====== Main Menu ======");
        for (int i = 0; i < categories.size(); i++) {
            System.out.printf("%d. %s%n", i + 1, categories.get(i).label);
        }
        System.out.println("0. Logout");
    }

    private List<MenuCategory> buildCategories() {
        List<MenuCategory> list = new ArrayList<>();
        list.add(new MenuCategory("User", List.of(
                "create-user", "list-users", "view-user", "update-user", "delete-user",
                "assign-role", "remove-role", "change-profile"
        )));
        list.add(new MenuCategory("Role", List.of(
                "create-role", "list-roles", "update-role", "delete-role"
        )));
        list.add(new MenuCategory("Permission", List.of(
                "create-permission", "list-permissions", "list-my-permissions",
                "update-permission", "delete-permission",
                "assign-permission", "remove-permission",
                "assign-resource-permission", "remove-resource-permission"
        )));
        list.add(new MenuCategory("Resource", List.of(
                "create-resource", "list-resources", "list-my-resources",
                "view-resource", "update-resource", "delete-resource"
        )));
        list.add(new MenuCategory("Audit", List.of(
                "view-audit", "view-all-audit", "view-user-audit",
                "view-action-audit", "view-resource-audit"
        )));
        list.add(new MenuCategory("Account", List.of(
                "view-profile", "change-password"
        )));
        return list;
    }

    private String describe(String command) {
        CommandSpec spec = CommandSpec.fromCommand(command);
        if (spec == null) {
            return null;
        }
        return spec.getDescription();
    }

    private static class MenuCategory {
        private final String label;
        private final List<String> commands;

        MenuCategory(String label, List<String> commands) {
            this.label = Objects.requireNonNull(label);
            this.commands = commands.stream().collect(Collectors.toList());
        }
    }
}

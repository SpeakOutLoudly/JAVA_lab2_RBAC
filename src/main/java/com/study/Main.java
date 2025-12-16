package com.study;

import com.study.config.CommandSpec;
import com.study.domain.Permission;
import com.study.domain.Role;
import com.study.domain.User;
import com.study.exception.RbacException;
import com.study.facade.RbacFacade;
import com.study.repository.DatabaseConnection;

import java.util.List;
import java.util.Scanner;

/**
 * Main entry point for RBAC CLI application
 */
public class Main {
    private static final Scanner scanner = new Scanner(System.in);
    private static RbacFacade facade;
    private static MenuState currentState = new GuestMenuState();
    
    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════╗");
        System.out.println("║  RBAC CLI System - Access Control     ║");
        System.out.println("╚════════════════════════════════════════╝");
        
        // Initialize database and facade
        DatabaseConnection dbConnection = DatabaseConnection.getInstance();
        facade = new RbacFacade(dbConnection);
        
        System.out.println("\nDefault admin credentials: admin / admin123\n");
        
        // Main loop
        boolean running = true;
        while (running) {
            try {
                currentState.displayMenu(facade);
                String command = readInput("Enter command: ");
                
                if ("exit".equalsIgnoreCase(command)) {
                    running = false;
                    System.out.println("\nThank you for using RBAC CLI System. Goodbye!");
                } else {
                    currentState.handleCommand(command, facade);
                }
            } catch (RbacException e) {
                System.out.println("\n❌ Error: " + e.getMessage());
            } catch (Exception e) {
                System.out.println("\n❌ Unexpected error: " + e.getMessage());
                e.printStackTrace();
            }
            System.out.println();
        }
        
        scanner.close();
    }
    
    // State pattern for menu management
    interface MenuState {
        void displayMenu(RbacFacade facade);
        void handleCommand(String command, RbacFacade facade);
    }
    
    // Guest menu (not logged in)
    static class GuestMenuState implements MenuState {
        @Override
        public void displayMenu(RbacFacade facade) {
            System.out.println("\n" + "═══════ GUEST MENU ═══════");
            System.out.println("  login  - Login to system");
            System.out.println("  exit   - Exit application");
            System.out.println("═══════════════════════════");
        }
        
        @Override
        public void handleCommand(String command, RbacFacade facade) {
            if ("login".equalsIgnoreCase(command)) {
                handleLogin(facade);
            } else {
                System.out.println("Unknown command. Available: login, exit");
            }
        }
        
        private void handleLogin(RbacFacade facade) {
            String username = readInput("Username: ");
            String password = readPassword("Password: ");
            
            User user = facade.login(username, password);
            System.out.println("\n✓ Login successful! Welcome, " + user.getUsername());
            currentState = new LoggedInMenuState();
        }
    }
    
    // Logged in menu
    static class LoggedInMenuState implements MenuState {
        @Override
        public void displayMenu(RbacFacade facade) {
            User currentUser = facade.getCurrentUser();
            System.out.println("\n" + "═══════ MAIN MENU ═══════");
            System.out.println("Logged in as: " + currentUser.getUsername());
            System.out.println("\nAvailable Commands:");
            
            List<CommandSpec> commands = facade.getAvailableCommands();
            for (CommandSpec spec : commands) {
                if (!spec.getCommand().equals("login") && !spec.getCommand().equals("exit")) {
                    System.out.printf("  %-20s - %s%n", spec.getCommand(), spec.getDescription());
                }
            }
            System.out.println("  exit                 - Exit application");
            System.out.println("═══════════════════════════");
        }
        
        @Override
        public void handleCommand(String command, RbacFacade facade) {
            switch (command.toLowerCase()) {
                case "logout" -> handleLogout(facade);
                case "change-password" -> handleChangePassword(facade);
                case "view-profile" -> handleViewProfile(facade);
                case "create-user" -> handleCreateUser(facade);
                case "list-users" -> handleListUsers(facade);
                case "list-roles" -> handleListRoles(facade);
                case "assign-role" -> handleAssignRole(facade);
                case "view-user" -> handleViewUser(facade);
                case "list-permissions" -> handleListPermissions(facade);
                default -> System.out.println("Unknown command: " + command);
            }
        }
        
        private void handleLogout(RbacFacade facade) {
            facade.logout();
            System.out.println("✓ Logged out successfully");
            currentState = new GuestMenuState();
        }
        
        private void handleChangePassword(RbacFacade facade) {
            String oldPassword = readPassword("Current password: ");
            String newPassword = readPassword("New password: ");
            String confirm = readPassword("Confirm new password: ");
            
            if (!newPassword.equals(confirm)) {
                System.out.println("❌ Passwords do not match");
                return;
            }
            
            facade.changePassword(oldPassword, newPassword);
            System.out.println("✓ Password changed successfully");
        }
        
        private void handleViewProfile(RbacFacade facade) {
            User user = facade.getCurrentUser();
            System.out.println("\n" + "─── User Profile ───");
            System.out.println("ID: " + user.getId());
            System.out.println("Username: " + user.getUsername());
            System.out.println("Status: " + (user.isEnabled() ? "Active" : "Disabled"));
            System.out.println("Created: " + user.getCreatedAt());
            
            List<Role> roles = facade.getUserRoles(user.getUsername());
            System.out.println("\nRoles: " + roles.stream().map(Role::getName).toList());
            
            List<Permission> permissions = facade.getUserPermissions(user.getUsername());
            System.out.println("Permissions (" + permissions.size() + "): " + 
                permissions.stream().map(Permission::getCode).limit(5).toList() + "...");
        }
        
        private void handleCreateUser(RbacFacade facade) {
            String username = readInput("New username: ");
            String password = readPassword("Password: ");
            String roleCode = readInput("Role code (optional, press Enter to skip): ");
            
            User user = roleCode.isBlank() ? 
                facade.createUser(username, password) :
                facade.createUserWithRole(username, password, roleCode);
            
            System.out.println("✓ User created: " + user.getUsername());
        }
        
        private void handleListUsers(RbacFacade facade) {
            List<User> users = facade.listUsers();
            System.out.println("\n" + "─── Users (" + users.size() + ") ───");
            for (User user : users) {
                System.out.printf("[%d] %s - %s%n", 
                    user.getId(), 
                    user.getUsername(), 
                    user.isEnabled() ? "Active" : "Disabled");
            }
        }
        
        private void handleListRoles(RbacFacade facade) {
            List<Role> roles = facade.listRoles();
            System.out.println("\n" + "─── Roles (" + roles.size() + ") ───");
            for (Role role : roles) {
                System.out.printf("[%d] %s - %s%n", 
                    role.getId(), 
                    role.getCode(), 
                    role.getName());
            }
        }
        
        private void handleAssignRole(RbacFacade facade) {
            String username = readInput("Username: ");
            String roleCode = readInput("Role code: ");
            
            facade.assignRoleToUser(username, roleCode);
            System.out.println("✓ Role assigned successfully");
        }
        
        private void handleViewUser(RbacFacade facade) {
            String username = readInput("Username: ");
            User user = facade.viewUser(username);
            
            System.out.println("\n" + "─── User Details ───");
            System.out.println("ID: " + user.getId());
            System.out.println("Username: " + user.getUsername());
            System.out.println("Status: " + (user.isEnabled() ? "Active" : "Disabled"));
            
            List<Role> roles = facade.getUserRoles(username);
            System.out.println("Roles: " + roles.stream().map(Role::getName).toList());
        }
        
        private void handleListPermissions(RbacFacade facade) {
            List<Permission> permissions = facade.listPermissions();
            System.out.println("\n" + "─── Permissions (" + permissions.size() + ") ───");
            for (Permission perm : permissions) {
                System.out.printf("[%d] %s - %s%n", 
                    perm.getId(), 
                    perm.getCode(), 
                    perm.getName());
            }
        }
    }
    
    // Utility methods
    private static String readInput(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }
    
    private static String readPassword(String prompt) {
        System.out.print(prompt);
        // In real application, use Console.readPassword() to hide input
        return scanner.nextLine().trim();
    }
}
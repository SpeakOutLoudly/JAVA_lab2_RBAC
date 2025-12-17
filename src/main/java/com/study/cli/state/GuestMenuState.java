package com.study.cli.state;

import com.study.common.util.InputUtils;
import com.study.domain.User;
import com.study.facade.RbacFacade;

/**
 * Guest menu state (not logged in).
 */
public class GuestMenuState implements MenuState {

    @Override
    public void displayMenu(RbacFacade facade) {
        System.out.println("\n=== Guest Menu ===");
        System.out.println("  login  - Sign in");
        System.out.println("  exit   - Exit application");
        System.out.println("==================");
    }

    @Override
    public void handleCommand(String command, RbacFacade facade) {
        if ("login".equalsIgnoreCase(command)) {
            handleLogin(facade);
        } else {
            System.out.println("Unknown command. Use: login, exit");
        }
    }

    private void handleLogin(RbacFacade facade) {
        String username = InputUtils.readInput("Username: ");
        String password = InputUtils.readPassword("Password: ");

        User user = facade.login(username, password);
        System.out.println("\n✔ Login successful, welcome " + user.getUsername() + ". Type 'list' to see commands.");
    }
}

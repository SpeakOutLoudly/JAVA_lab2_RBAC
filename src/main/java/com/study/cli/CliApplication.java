package com.study.cli;

import com.study.cli.state.GuestMenuState;
import com.study.cli.state.LoggedInMenuState;
import com.study.cli.state.MenuState;
import com.study.common.util.InputUtils;
import com.study.exception.RbacException;
import com.study.facade.RbacFacade;

/**
 * CLI Application - handles command line interaction.
 */
public class CliApplication {
    private final RbacFacade facade;
    private MenuState currentState;
    private final boolean debugEnabled = Boolean.getBoolean("cli.debug");

    public CliApplication(RbacFacade facade) {
        this.facade = facade;
        this.currentState = new GuestMenuState();
    }

    /**
     * Start the CLI application.
     */
    public void start() {
        System.out.println("==============================");
        System.out.println("  RBAC CLI - Access Control  ");
        System.out.println("==============================");
        System.out.println("\nDefault admin: admin / admin123\n");

        currentState.displayMenu(facade);
        boolean running = true;
        while (running) {
            try {
                String command = InputUtils.readInput("Command (type 'list' to show menu): ");

                if ("list".equalsIgnoreCase(command)) {
                    currentState.displayMenu(facade);
                    continue;
                }
                if ("exit".equalsIgnoreCase(command)) {
                    running = false;
                    System.out.println("\nThanks for using RBAC CLI. Bye!");
                } else if ("login".equalsIgnoreCase(command) && currentState instanceof GuestMenuState) {
                    currentState.handleCommand(command, facade);
                    if (facade.isLoggedIn()) {
                        currentState = new LoggedInMenuState();
                    }
                } else if ("logout".equalsIgnoreCase(command) && currentState instanceof LoggedInMenuState) {
                    currentState.handleCommand(command, facade);
                    currentState = new GuestMenuState();
                } else {
                    currentState.handleCommand(command, facade);
                }
            } catch (RbacException e) {
                System.out.println("\n⚠ Error: " + e.getMessage());
            } catch (Exception e) {
                System.out.println("\n⚠ Internal error: " + e.getMessage());
                if (debugEnabled) {
                    e.printStackTrace();
                }
            }
            System.out.println();
        }

        InputUtils.close();
    }
}

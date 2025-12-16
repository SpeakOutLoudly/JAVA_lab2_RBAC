package com.study.cli;

import com.study.cli.state.GuestMenuState;
import com.study.cli.state.LoggedInMenuState;
import com.study.cli.state.MenuState;
import com.study.common.util.InputUtils;
import com.study.exception.RbacException;
import com.study.facade.RbacFacade;

/**
 * CLI Application - handles command line interaction
 */
public class CliApplication {
    private final RbacFacade facade;
    private MenuState currentState;
    
    public CliApplication(RbacFacade facade) {
        this.facade = facade;
        this.currentState = new GuestMenuState();
    }
    
    /**
     * Start the CLI application
     */
    public void start() {
        System.out.println("╔════════════════════════════════════════╗");
        System.out.println("║     RBAC CLI 系统 - 访问控制             ║");
        System.out.println("╚════════════════════════════════════════╝");
        System.out.println("\n默认管理员账号: admin / admin123\n");

        currentState.displayMenu(facade);
        boolean running = true;
        while (running) {
            try {

                String command = InputUtils.readInput("请输入命令: ");

                if("list".equalsIgnoreCase( command)) {
                    currentState.displayMenu(facade);
                    continue;
                }
                if ("exit".equalsIgnoreCase(command)) {
                    running = false;
                    System.out.println("\n感谢使用 RBAC CLI 系统，再见！");
                } else if ("login".equalsIgnoreCase(command) && currentState instanceof GuestMenuState) {
                    currentState.handleCommand(command, facade);
                    // Switch to logged in state after successful login
                    if (facade.isLoggedIn()) {
                        currentState = new LoggedInMenuState();
                    }
                } else if ("logout".equalsIgnoreCase(command) && currentState instanceof LoggedInMenuState) {
                    currentState.handleCommand(command, facade);
                    // Switch back to guest state after logout
                    currentState = new GuestMenuState();
                } else {
                    currentState.handleCommand(command, facade);
                }
            } catch (RbacException e) {
                System.out.println("\n❌ 错误: " + e.getMessage());
            } catch (Exception e) {
                System.out.println("\n❌ 未预期的错误: " + e.getMessage());
                e.printStackTrace();
            }
            System.out.println();
        }
        
        InputUtils.close();
    }
}

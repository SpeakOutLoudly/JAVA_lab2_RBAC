package com.study.cli;

import com.study.cli.state.GuestMenuState;
import com.study.cli.state.LoggedInMenuState;
import com.study.cli.state.MenuState;
import com.study.common.util.InputUtils;
import com.study.exception.RbacException;
import com.study.facade.RbacFacade;

/**
 * CLI Application - handles command line interaction with menu navigation.
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

        boolean running = true;
        while (running) {
            try {
                currentState.displayMenu(facade);
                String input = InputUtils.readInput("请选择 [输入数字]: ").trim();

                if (input.isEmpty()) {
                    continue;
                }

                // 处理退出
                if ("0".equals(input) && currentState instanceof GuestMenuState) {
                    running = false;
                    System.out.println("\n感谢使用 RBAC CLI，再见！");
                    continue;
                }

                // 处理菜单选择
                MenuState nextState = currentState.handleMenuChoice(input, facade);
                if (nextState != null) {
                    currentState = nextState;
                }

            } catch (RbacException e) {
                System.out.println("\n[错误] " + e.getMessage());
            } catch (Exception e) {
                System.out.println("\n[错误] 内部错误: " + e.getMessage());
                if (debugEnabled) {
                    e.printStackTrace();
                }
            }
            System.out.println();
        }

        InputUtils.close();
    }
}

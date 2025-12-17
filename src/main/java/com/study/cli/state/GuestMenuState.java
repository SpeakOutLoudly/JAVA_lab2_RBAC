package com.study.cli.state;

import com.study.common.util.InputUtils;
import com.study.domain.User;
import com.study.facade.RbacFacade;

/**
 * Guest menu state (not logged in) - number-based menu.
 */
public class GuestMenuState implements MenuState {

    @Override
    public void displayMenu(RbacFacade facade) {
        System.out.println("\n========== 主菜单 ==========");
        System.out.println("1. 登录");
        System.out.println("0. 退出系统");
        System.out.println("==========================");
    }

    @Override
    public MenuState handleMenuChoice(String input, RbacFacade facade) {
        switch (input) {
            case "1" -> {
                return handleLogin(facade);
            }
            case "0" -> {
                // 退出由 CliApplication 处理
                return null;
            }
            default -> {
                System.out.println("[错误] 无效的选择，请输入 1 或 0");
                return null;
            }
        }
    }

    private MenuState handleLogin(RbacFacade facade) {
        String username = InputUtils.readInput("用户名: ");
        String password = InputUtils.readPassword("password: ");

        User user = facade.login(username, password);
        System.out.println("\n✔ 登录成功，欢迎 " + user.getUsername());
        return new LoggedInMenuState();
    }
}

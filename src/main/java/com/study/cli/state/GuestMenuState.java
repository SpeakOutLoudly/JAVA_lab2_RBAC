package com.study.cli.state;

import com.study.common.util.InputUtils;
import com.study.domain.User;
import com.study.facade.RbacFacade;

/**
 * Guest menu state (not logged in)
 */
public class GuestMenuState implements MenuState {
    
    @Override
    public void displayMenu(RbacFacade facade) {
        System.out.println("\n" + "═══════ 访客菜单 ═══════");
        System.out.println("  login  - 用户登录");
        System.out.println("  exit   - 退出系统");
        System.out.println("═══════════════════════════");
    }
    
    @Override
    public void handleCommand(String command, RbacFacade facade) {
        if ("login".equalsIgnoreCase(command)) {
            handleLogin(facade);
        } else {
            System.out.println("未知命令。可用命令: login, exit");
        }
    }
    
    private void handleLogin(RbacFacade facade) {
        String username = InputUtils.readInput("用户名: ");
        String password = InputUtils.readPassword("password: ");
        
        User user = facade.login(username, password);
        System.out.println("\n✓ 登录成功！欢迎, " + user.getUsername() + ", 可用过 list 查看指令");
    }
}

package com.study.cli.handler;

import com.study.common.util.InputUtils;
import com.study.domain.Permission;
import com.study.domain.Role;
import com.study.domain.User;
import com.study.facade.RbacFacade;

import java.util.List;

/**
 * Handler for authentication related commands
 */
public class AuthCommandHandler implements CommandHandler {
    
    @Override
    public void handle(String command, RbacFacade facade) {
        switch (command) {
            case "logout" -> handleLogout(facade);
            case "change-password" -> handleChangePassword(facade);
            case "view-profile" -> handleViewProfile(facade);
            default -> System.out.println("未知的认证命令: " + command);
        }
    }
    
    private void handleLogout(RbacFacade facade) {
        facade.logout();
        System.out.println("✓ 登出成功");
    }
    
    private void handleChangePassword(RbacFacade facade) {
        String oldPassword = InputUtils.readPassword("当前密码: ");
        String newPassword = InputUtils.readPassword("新密码: ");
        String confirm = InputUtils.readPassword("确认新密码: ");
        
        if (!newPassword.equals(confirm)) {
            System.out.println("❌ 密码不匹配");
            return;
        }
        
        facade.changePassword(oldPassword, newPassword);
        System.out.println("✓ 密码修改成功");
    }
    
    private void handleViewProfile(RbacFacade facade) {
        User user = facade.getCurrentUser();
        System.out.println("\n" + "─── 用户资料 ───");
        System.out.println("ID: " + user.getId());
        System.out.println("用户名: " + user.getUsername());
        System.out.println("状态: " + (user.isEnabled() ? "启用" : "禁用"));
        System.out.println("创建时间: " + user.getCreatedAt());
        
        List<Role> roles = facade.getUserRoles(user.getUsername());
        System.out.println("\n角色: " + roles.stream().map(Role::getName).toList());
        
        List<Permission> permissions = facade.getUserPermissions(user.getUsername());
        System.out.println("权限 (" + permissions.size() + "): " + 
            permissions.stream().map(Permission::getCode).limit(5).toList() + "...");
    }
}

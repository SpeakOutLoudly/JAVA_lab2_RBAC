package com.study.cli.handler;

import com.study.common.util.InputUtils;
import com.study.domain.Role;
import com.study.domain.User;
import com.study.facade.RbacFacade;

import java.util.List;

/**
 * Handler for user management commands
 */
public class UserCommandHandler implements CommandHandler {
    
    @Override
    public void handle(String command, RbacFacade facade) {
        switch (command) {
            case "create-user" -> handleCreateUser(facade);
            case "list-users" -> handleListUsers(facade);
            case "view-user" -> handleViewUser(facade);
            default -> System.out.println("未知的用户命令: " + command);
        }
    }
    
    private void handleCreateUser(RbacFacade facade) {
        String username = InputUtils.readInput("新用户名: ");
        String password = InputUtils.readPassword("密码: ");
        String roleCode = InputUtils.readInput("角色代码 (可选，按回车跳过): ");
        
        User user = roleCode.isBlank() ? 
            facade.createUser(username, password) :
            facade.createUserWithRole(username, password, roleCode);
        
        System.out.println("✓ 用户创建成功: " + user.getUsername());
    }
    
    private void handleListUsers(RbacFacade facade) {
        List<User> users = facade.listUsers();
        System.out.println("\n" + "─── 用户列表 (" + users.size() + ") ───");
        for (User user : users) {
            System.out.printf("[%d] %s - %s%n", 
                user.getId(), 
                user.getUsername(), 
                user.isEnabled() ? "启用" : "禁用");
        }
    }
    
    private void handleViewUser(RbacFacade facade) {
        String username = InputUtils.readInput("用户名: ");
        User user = facade.viewUser(username);
        
        System.out.println("\n" + "─── 用户详情 ───");
        System.out.println("ID: " + user.getId());
        System.out.println("用户名: " + user.getUsername());
        System.out.println("状态: " + (user.isEnabled() ? "启用" : "禁用"));
        
        List<Role> roles = facade.getUserRoles(username);
        System.out.println("角色: " + roles.stream().map(Role::getName).toList());
    }
}

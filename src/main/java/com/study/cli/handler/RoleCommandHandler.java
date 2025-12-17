package com.study.cli.handler;

import com.study.common.util.InputUtils;
import com.study.domain.Role;
import com.study.facade.RbacFacade;

import java.util.List;

/**
 * Handler for role management commands
 */
public class RoleCommandHandler implements CommandHandler {
    
    @Override
    public void handle(String command, RbacFacade facade) {
        switch (command) {
            case "list-roles" -> handleListRoles(facade);
            case "assign-role" -> handleAssignRole(facade);
            default -> System.out.println("未知的角色命令: " + command);
        }
    }
    
    private void handleListRoles(RbacFacade facade) {
        List<Role> roles = facade.listRoles();
        System.out.println("\n" + "─── 角色列表 (" + roles.size() + ") ───");
        for (Role role : roles) {
            System.out.printf("[%d] %s - %s%n", 
                role.getId(), 
                role.getCode(), 
                role.getName());
        }
    }
    
    private void handleAssignRole(RbacFacade facade) {
        String username = InputUtils.readInput("用户名: ");
        String roleCode = InputUtils.readInput("角色代码: ");
        
        facade.assignRoleToUser(username, roleCode);
        System.out.println("✓ 角色分配成功");
    }
}

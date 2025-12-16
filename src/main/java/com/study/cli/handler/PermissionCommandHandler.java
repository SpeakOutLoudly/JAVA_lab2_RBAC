package com.study.cli.handler;

import com.study.domain.Permission;
import com.study.facade.RbacFacade;

import java.util.List;

/**
 * Handler for permission management commands
 */
public class PermissionCommandHandler implements CommandHandler {
    
    @Override
    public void handle(String command, RbacFacade facade) {
        if ("list-permissions".equals(command)) {
            handleListPermissions(facade);
        } else {
            System.out.println("未知的权限命令: " + command);
        }
    }
    
    private void handleListPermissions(RbacFacade facade) {
        List<Permission> permissions = facade.listPermissions();
        System.out.println("\n" + "─── 权限列表 (" + permissions.size() + ") ───");
        for (Permission perm : permissions) {
            System.out.printf("[%d] %s - %s%n", 
                perm.getId(), 
                perm.getCode(), 
                perm.getName());
        }
    }
}

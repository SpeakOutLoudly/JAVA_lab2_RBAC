package com.study.cli.state;

import com.study.cli.handler.*;
import com.study.config.CommandSpec;
import com.study.domain.User;
import com.study.facade.RbacFacade;

import java.util.*;

/**
 * Logged in menu state - number-based hierarchical menu navigation.
 */
public class LoggedInMenuState implements MenuState {
    private final Map<String, CommandHandler> handlers = new HashMap<>();
    private String currentSubMenu = null; // null = main menu, otherwise submenu name

    public LoggedInMenuState() {
        CommandHandler authHandler = new AuthCommandHandler();
        CommandHandler userHandler = new UserCommandHandler();
        CommandHandler roleHandler = new RoleCommandHandler();
        CommandHandler permissionHandler = new PermissionCommandHandler();
        CommandHandler resourceHandler = new ResourceCommandHandler();
        CommandHandler auditHandler = new AuditCommandHandler();

        handlers.put("logout", authHandler);
        handlers.put("change-password", authHandler);
        handlers.put("view-profile", authHandler);

        handlers.put("create-user", userHandler);
        handlers.put("list-users", userHandler);
        handlers.put("view-user", userHandler);
        handlers.put("delete-user", userHandler);
        handlers.put("update-user", userHandler);
        handlers.put("assign-role", roleHandler);
        handlers.put("remove-role", roleHandler);

        handlers.put("create-role", roleHandler);
        handlers.put("list-roles", roleHandler);
        handlers.put("update-role", roleHandler);
        handlers.put("delete-role", roleHandler);

        handlers.put("create-permission", permissionHandler);
        handlers.put("list-permissions", permissionHandler);
        handlers.put("list-my-permissions", permissionHandler);
        handlers.put("assign-permission", permissionHandler);
        handlers.put("remove-permission", permissionHandler);
        handlers.put("update-permission", permissionHandler);
        handlers.put("delete-permission", permissionHandler);
        handlers.put("assign-resource-permission", permissionHandler);
        handlers.put("remove-resource-permission", permissionHandler);

        handlers.put("create-resource", resourceHandler);
        handlers.put("list-resources", resourceHandler);
        handlers.put("view-resource", resourceHandler);
        handlers.put("update-resource", resourceHandler);
        handlers.put("delete-resource", resourceHandler);

        handlers.put("view-audit", auditHandler);
        handlers.put("view-all-audit", auditHandler);
        handlers.put("view-user-audit", auditHandler);
        handlers.put("view-action-audit", auditHandler);
        handlers.put("view-resource-audit", auditHandler);
    }

    @Override
    public void displayMenu(RbacFacade facade) {
        if (currentSubMenu == null) {
            displayMainMenu(facade);
        } else {
            displaySubMenu(currentSubMenu, facade);
        }
    }

    private void displayMainMenu(RbacFacade facade) {
        User currentUser = facade.getCurrentUser();
        System.out.println("\n========== 主菜单 ==========");
        System.out.println("当前用户: " + currentUser.getUsername());
        System.out.println();
        System.out.println("1. 用户管理");
        System.out.println("2. 角色管理");
        System.out.println("3. 权限管理");
        System.out.println("4. 资源管理");
        System.out.println("5. 审计查询");
        System.out.println("6. 账户管理");
        System.out.println("0. 退出登录");
        System.out.println("==========================");
    }

    private void displaySubMenu(String menuName, RbacFacade facade) {
        System.out.println("\n========== " + menuName + " ==========");
        
        Map<Integer, String> menuItems = getSubMenuItems(menuName, facade);
        for (Map.Entry<Integer, String> entry : menuItems.entrySet()) {
            System.out.println(entry.getKey() + ". " + entry.getValue());
        }
        System.out.println("0. 返回上级菜单");
        System.out.println("==========================");
    }

    private Map<Integer, String> getSubMenuItems(String menuName, RbacFacade facade) {
        // 获取所有可能的菜单项
        List<String> allCommands = new ArrayList<>();
        
        switch (menuName) {
            case "用户管理" -> {
                allCommands.add("创建用户 (create-user)");
                allCommands.add("查看用户列表 (list-users)");
                allCommands.add("查看用户详情 (view-user)");
                allCommands.add("修改用户 (update-user)");
                allCommands.add("删除用户 (delete-user)");
                allCommands.add("分配角色 (assign-role)");
                allCommands.add("移除角色 (remove-role)");
            }
            case "角色管理" -> {
                allCommands.add("创建角色 (create-role)");
                allCommands.add("查看角色列表 (list-roles)");
                allCommands.add("修改角色 (update-role)");
                allCommands.add("删除角色 (delete-role)");
            }
            case "权限管理" -> {
                allCommands.add("创建权限 (create-permission)");
                allCommands.add("查看权限列表 (list-permissions)");
                allCommands.add("查看我的权限 (list-my-permissions)");
                allCommands.add("修改权限 (update-permission)");
                allCommands.add("删除权限 (delete-permission)");
                allCommands.add("分配权限到角色 (assign-permission)");
                allCommands.add("移除角色权限 (remove-permission)");
                allCommands.add("分配资源权限 (assign-resource-permission)");
                allCommands.add("移除资源权限 (remove-resource-permission)");
            }
            case "资源管理" -> {
                allCommands.add("创建资源 (create-resource)");
                allCommands.add("查看资源列表 (list-resources)");
                allCommands.add("查看资源详情 (view-resource)");
                allCommands.add("修改资源 (update-resource)");
                allCommands.add("删除资源 (delete-resource)");
            }
            case "审计查询" -> {
                allCommands.add("查看我的审计日志 (view-audit)");
                allCommands.add("查看所有审计日志 (view-all-audit)");
                allCommands.add("按用户查询 (view-user-audit)");
                allCommands.add("按操作查询 (view-action-audit)");
                allCommands.add("按资源查询 (view-resource-audit)");
            }
            case "账户管理" -> {
                allCommands.add("查看个人信息 (view-profile)");
                allCommands.add("修改密码 (change-password)");
            }
        }
        
        // 过滤出用户有权限的命令，并重新编号
        Map<Integer, String> items = new LinkedHashMap<>();
        int index = 1;
        for (String commandItem : allCommands) {
            String command = extractCommand(commandItem);
            if (command != null && facade.canExecuteCommand(command)) {
                items.put(index++, commandItem);
            }
        }
        
        return items;
    }
    
    private String extractCommand(String item) {
        // 从 "创建用户 (create-user)" 中提取 "create-user"
        int startIdx = item.indexOf("(");
        int endIdx = item.indexOf(")");
        if (startIdx != -1 && endIdx != -1) {
            return item.substring(startIdx + 1, endIdx);
        }
        return null;
    }

    private String getCommandByMenuItem(String menuName, int choice, RbacFacade facade) {
        Map<Integer, String> items = getSubMenuItems(menuName, facade);
        String item = items.get(choice);
        if (item == null) return null;
        
        return extractCommand(item);
    }

    @Override
    public MenuState handleMenuChoice(String input, RbacFacade facade) {
        try {
            int choice = Integer.parseInt(input);
            
            if (currentSubMenu == null) {
                // 在主菜单
                return handleMainMenuChoice(choice, facade);
            } else {
                // 在子菜单
                return handleSubMenuChoice(choice, facade);
            }
        } catch (NumberFormatException e) {
            System.out.println("[错误] 请输入有效的数字");
            return null;
        }
    }

    private MenuState handleMainMenuChoice(int choice, RbacFacade facade) {
        switch (choice) {
            case 1 -> {
                currentSubMenu = "用户管理";
                return null;
            }
            case 2 -> {
                currentSubMenu = "角色管理";
                return null;
            }
            case 3 -> {
                currentSubMenu = "权限管理";
                return null;
            }
            case 4 -> {
                currentSubMenu = "资源管理";
                return null;
            }
            case 5 -> {
                currentSubMenu = "审计查询";
                return null;
            }
            case 6 -> {
                currentSubMenu = "账户管理";
                return null;
            }
            case 0 -> {
                // 退出登录
                CommandHandler handler = handlers.get("logout");
                if (handler != null) {
                    handler.handle("logout", facade);
                }
                return new GuestMenuState();
            }
            default -> {
                System.out.println("[错误] 无效的选择，请输入 0-6");
                return null;
            }
        }
    }

    private MenuState handleSubMenuChoice(int choice, RbacFacade facade) {
        if (choice == 0) {
            // 返回主菜单
            currentSubMenu = null;
            return null;
        }

        String command = getCommandByMenuItem(currentSubMenu, choice, facade);
        if (command != null) {
            // 二次权限校验（安全必须，即使菜单已过滤）
            if (!facade.canExecuteCommand(command)) {
                CommandSpec spec = CommandSpec.fromCommand(command);
                String requiredPerm = spec != null && spec.getRequiredPermission() != null 
                    ? spec.getRequiredPermission() 
                    : "未知权限";
                System.out.println("[错误] Permission denied: " + command.toUpperCase().replace("-", "_") 
                    + " (required: " + requiredPerm + ")");
                return null;
            }
            
            CommandHandler handler = handlers.get(command);
            if (handler != null) {
                handler.handle(command, facade);
            } else {
                System.out.println("[错误] 未找到命令处理器: " + command);
            }
        } else {
            System.out.println("[错误] 无效的选择");
        }

        return null; // 留在当前子菜单
    }
}

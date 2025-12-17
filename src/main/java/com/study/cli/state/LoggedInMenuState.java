package com.study.cli.state;

import com.study.cli.handler.*;
import com.study.config.CommandSpec;
import com.study.domain.User;
import com.study.facade.RbacFacade;

import java.util.*;

/**
 * Logged in menu state.
 */
public class LoggedInMenuState implements MenuState {
    private final Map<String, CommandHandler> handlers = new HashMap<>();

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

        handlers.put("create-role", roleHandler);
        handlers.put("list-roles", roleHandler);
        handlers.put("assign-role", roleHandler);
        handlers.put("remove-role", roleHandler);
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
        User currentUser = facade.getCurrentUser();
        System.out.println("\n===== Main Menu =====");
        System.out.println("Logged in as: " + currentUser.getUsername());
        System.out.println();

        List<CommandSpec> commands = facade.getAvailableCommands().stream()
                .filter(c -> !"login".equals(c.getCommand())) // 登录态菜单不显示 login
                .toList();

        // 先把命令分组（你也可以改成"显式映射表"，更可控）
        Map<String, List<CommandSpec>> groups = new LinkedHashMap<>();
        groups.put("User", new ArrayList<>());
        groups.put("Role", new ArrayList<>());
        groups.put("Permission", new ArrayList<>());
        groups.put("Resource", new ArrayList<>());
        groups.put("Audit", new ArrayList<>());
        groups.put("Account", new ArrayList<>());
        groups.put("System", new ArrayList<>());

        for (CommandSpec c : commands) {
            String cmd = c.getCommand();
            if ("exit".equals(cmd) || "logout".equals(cmd)) {
                groups.get("System").add(c);
            } else if (cmd.contains("audit")) {
                groups.get("Audit").add(c);
            } else if (cmd.contains("resource") || cmd.contains("resources")) {
                groups.get("Resource").add(c);
            } else if (cmd.contains("permission")) {
                groups.get("Permission").add(c);
            } else if (cmd.contains("role") && !cmd.contains("assign-role") && !cmd.contains("remove-role")) {
                groups.get("Role").add(c);
            } else if (cmd.contains("user") || cmd.contains("users") || "assign-role".equals(cmd) || "remove-role".equals(cmd)) {
                groups.get("User").add(c);
            } else if (cmd.contains("profile") || cmd.contains("password")) {
                groups.get("Account").add(c);
            } else {
                // 兜底：不认识的命令放系统或单独 Unknown
                groups.get("System").add(c);
            }
        }

        // 排序：同组内按命令名排序（也可按 create/list/view/update/delete 自定义顺序）
        for (List<CommandSpec> list : groups.values()) {
            list.sort(Comparator.comparing(CommandSpec::getCommand));
        }

        // 计算命令列宽：按所有命令最长值 + padding，对齐更整齐
        int cmdWidth = commands.stream()
                .mapToInt(c -> c.getCommand().length())
                .max()
                .orElse(16);
        cmdWidth = Math.max(cmdWidth, 16) + 2; // 至少 16，留 2 个空格

        for (var entry : groups.entrySet()) {
            List<CommandSpec> list = entry.getValue();
            if (list.isEmpty()) continue;

            System.out.println("[" + entry.getKey() + "]");
            for (CommandSpec c : list) {
                // 两列：命令列固定宽度，描述列自然换行（不截断）
                System.out.printf("  %-" + cmdWidth + "s%s%n", c.getCommand(), c.getDescription());
            }
            System.out.println();
        }

        System.out.println("=====================\n");
    }


    @Override
    public void handleCommand(String command, RbacFacade facade) {
        CommandHandler handler = handlers.get(command.toLowerCase());
        if (handler != null) {
            handler.handle(command.toLowerCase(), facade);
        } else {
            System.out.println("Unknown command: " + command);
        }
    }
}

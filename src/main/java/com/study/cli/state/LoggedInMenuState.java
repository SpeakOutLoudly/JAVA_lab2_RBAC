package com.study.cli.state;

import com.study.cli.handler.*;
import com.study.config.CommandSpec;
import com.study.domain.User;
import com.study.facade.RbacFacade;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Logged in menu state
 */
public class LoggedInMenuState implements MenuState {
    private final Map<String, CommandHandler> handlers = new HashMap<>();
    
    public LoggedInMenuState() {
        // Register command handlers
        handlers.put("logout", new AuthCommandHandler());
        handlers.put("change-password", new AuthCommandHandler());
        handlers.put("view-profile", new AuthCommandHandler());
        handlers.put("create-user", new UserCommandHandler());
        handlers.put("list-users", new UserCommandHandler());
        handlers.put("view-user", new UserCommandHandler());
        handlers.put("assign-role", new RoleCommandHandler());
        handlers.put("list-roles", new RoleCommandHandler());
        handlers.put("list-permissions", new PermissionCommandHandler());
    }
    @Override
    public void displayMenu(RbacFacade facade) {
        User currentUser = facade.getCurrentUser();
        System.out.println("\n" + "═══════ 主菜单 ═══════");
        System.out.println("登录用户: " + currentUser.getUsername());
        System.out.println("\n可用命令:");
        
        List<CommandSpec> commands = facade.getAvailableCommands();
        int count = 0;
        StringBuilder line = new StringBuilder();
        
        for (CommandSpec spec : commands) {
            if (!spec.getCommand().equals("login") && !spec.getCommand().equals("exit")) {
                // Format: command(description)
                String item = String.format("%-22s", spec.getCommand() + "(" + spec.getDescription() + ")");
                line.append(item);
                count++;
                
                if (count % 3 == 0) {
                    System.out.println("  " + line.toString());
                    line = new StringBuilder();
                }
            }
        }
        
        // Add exit command to the current line
        String exitItem = String.format("%-22s", "exit(退出系统)");
        line.append(exitItem);
        count++;
        
        // Print remaining items
        if (line.length() > 0) {
            System.out.println("  " + line.toString());
        }
        
        System.out.println("═══════════════════════════");
    }
    
    @Override
    public void handleCommand(String command, RbacFacade facade) {
        CommandHandler handler = handlers.get(command.toLowerCase());
        if (handler != null) {
            handler.handle(command.toLowerCase(), facade);
        } else {
            System.out.println("未知命令: " + command);
        }
    }
}

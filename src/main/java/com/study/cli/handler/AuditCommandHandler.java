package com.study.cli.handler;

import com.study.common.util.InputUtils;
import com.study.domain.AuditLog;
import com.study.facade.RbacFacade;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Handler for audit log commands.
 */
public class AuditCommandHandler implements CommandHandler {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int DEFAULT_LIMIT = 50;

    @Override
    public void handle(String command, RbacFacade facade) {
        switch (command) {
            case "view-audit" -> displayLogs(facade.viewMyAuditLogs(readLimit()));
            case "view-all-audit" -> displayLogs(facade.viewAllAuditLogs(readLimit()));
            case "view-user-audit" -> handleViewUserAudit(facade);
            case "view-action-audit" -> handleViewActionAudit(facade);
            case "view-resource-audit" -> handleViewResourceAudit(facade);
            default -> System.out.println("Unknown audit command: " + command);
        }
    }

    private int readLimit() {
        return InputUtils.readIntOrDefault("Number of records (default 50): ", DEFAULT_LIMIT);
    }

    private void handleViewUserAudit(RbacFacade facade) {
        long userId = InputUtils.readLong("User ID: ");
        displayLogs(facade.viewUserAuditLogs(userId, readLimit()));
    }

    private void handleViewActionAudit(RbacFacade facade) {
        String action = InputUtils.readInput("Action keyword: ");
        displayLogs(facade.viewAuditLogsByAction(action, readLimit()));
    }

    private void handleViewResourceAudit(RbacFacade facade) {
        String resourceType = InputUtils.readInput("Resource type: ");
        String resourceId = InputUtils.readInput("Resource ID (blank for all): ");
        displayLogs(facade.viewAuditLogsByResource(resourceType, resourceId.isBlank() ? null : resourceId, readLimit()));
    }

    private void displayLogs(List<AuditLog> logs) {
        System.out.println("\n== Audit Logs (" + logs.size() + ") ==");
        if (logs.isEmpty()) {
            System.out.println("No audit records found.");
            return;
        }
        logs.forEach(log -> {
            String timestamp = log.getCreatedAt().format(FORMATTER);
            String username = log.getUsername() != null ? log.getUsername() : "N/A";
            String result = log.isSuccess() ? "SUCCESS" : "FAILED";
            String detail = log.isSuccess() ? log.getDetail() : log.getErrorMessage();
            System.out.printf("%s | %s | %s | %s | %s | %s%n",
                    timestamp,
                    username,
                    log.getAction(),
                    log.getResourceType() != null ? log.getResourceType() : "-",
                    log.getResourceId() != null ? log.getResourceId() : "-",
                    result + (detail != null ? " (" + detail + ")" : ""));
        });
    }
}

package com.study.cli.handler;

import com.study.common.util.InputUtils;
import com.study.domain.Resource;
import com.study.facade.RbacFacade;

import java.util.List;

/**
 * Handler for resource management commands.
 */
public class ResourceCommandHandler implements CommandHandler {

    @Override
    public void handle(String command, RbacFacade facade) {
        switch (command) {
            case "create-resource" -> handleCreateResource(facade);
            case "list-resources" -> handleListResources(facade);
            case "view-resource" -> handleViewResource(facade);
            case "update-resource" -> handleUpdateResource(facade);
            case "delete-resource" -> handleDeleteResource(facade);
            default -> System.out.println("Unknown resource command: " + command);
        }
    }

    private void handleCreateResource(RbacFacade facade) {
        String code = InputUtils.readInput("Resource code: ");
        String name = InputUtils.readInput("Resource name: ");
        String type = InputUtils.readInput("Resource type: ");
        String url = InputUtils.readInput("Resource url (optional): ");

        Resource resource = facade.createResource(code, name, type, url.isBlank() ? null : url);
        System.out.println("✔ Resource created: " + resource.getCode());
    }

    private void handleListResources(RbacFacade facade) {
        List<Resource> resources = facade.listResources();
        System.out.println("\n== Resources (" + resources.size() + ") ==");
        resources.forEach(r ->
                System.out.printf("[%d] %s (%s)%n", r.getId(), r.getCode(), r.getType())
        );
    }

    private void handleViewResource(RbacFacade facade) {
        long resourceId = InputUtils.readLong("Resource ID: ");
        Resource resource = facade.getResource(resourceId);
        System.out.println("\n== Resource Detail ==");
        System.out.println("ID: " + resource.getId());
        System.out.println("Code: " + resource.getCode());
        System.out.println("Name: " + resource.getName());
        System.out.println("Type: " + resource.getType());
        System.out.println("URL: " + (resource.getUrl() != null ? resource.getUrl() : "-"));
    }

    private void handleUpdateResource(RbacFacade facade) {
        long resourceId = InputUtils.readLong("Resource ID: ");
        String name = InputUtils.readInput("New name (leave blank to keep): ");
        String type = InputUtils.readInput("New type (leave blank to keep): ");
        String url = InputUtils.readInput("New url (leave blank to keep): ");

        Resource resource = facade.updateResource(resourceId,
                name.isBlank() ? null : name,
                type.isBlank() ? null : type,
                url.isBlank() ? null : url);
        System.out.println("✔ Resource updated: " + resource.getCode());
    }

    private void handleDeleteResource(RbacFacade facade) {
        long resourceId = InputUtils.readLong("Resource ID: ");
        String confirm = InputUtils.readInput("Confirm delete? (yes/no): ");
        if ("yes".equalsIgnoreCase(confirm)) {
            facade.deleteResource(resourceId);
            System.out.println("✔ Resource deleted");
        } else {
            System.out.println("Delete cancelled.");
        }
    }
}

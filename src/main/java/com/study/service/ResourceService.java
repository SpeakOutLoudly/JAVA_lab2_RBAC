package com.study.service;

import com.study.config.PermissionCodes;
import com.study.context.SessionContext;
import com.study.domain.Resource;
import com.study.domain.ScopedPermission;
import com.study.domain.User;
import com.study.exception.ValidationException;
import com.study.repository.AuditLogRepository;
import com.study.repository.PermissionRepository;
import com.study.repository.ResourceRepository;
import com.study.service.dto.ResourceAccessView;
import com.study.service.dto.ResourceRoleScope;
import com.study.service.dto.ResourceUserScope;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for managing resources.
 */
public class ResourceService extends BaseService {
    private final ResourceRepository resourceRepository;
    private final PermissionRepository permissionRepository;

    public ResourceService(SessionContext sessionContext,
                           ResourceRepository resourceRepository,
                           PermissionRepository permissionRepository,
                           AuditLogRepository auditLogRepository) {
        super(sessionContext, auditLogRepository);
        this.resourceRepository = resourceRepository;
        this.permissionRepository = permissionRepository;
    }

    public Resource createResource(String code, String name, String type, String url) {
        return executeWithTemplate(
                PermissionCodes.RESOURCE_CREATE,
                "CREATE_RESOURCE",
                type,
                code,
                () -> {
                    validateNotBlank(code, "Resource code");
                    validateNotBlank(name, "Resource name");
                    validateNotBlank(type, "Resource type");
                },
                () -> {
                    Resource resource = new Resource();
                    resource.setCode(code);
                    resource.setName(name);
                    resource.setType(type);
                    resource.setUrl(url);
                    return resourceRepository.save(resource);
                }
        );
    }

    public Resource updateResource(Long id, String name, String type, String url) {
        validateNotNull(id, "Resource ID");
        Resource target = resourceRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Resource not found: " + id));

        return executeWithTemplate(
                PermissionCodes.RESOURCE_UPDATE,
                "UPDATE_RESOURCE",
                target.getType(),
                id.toString(),
                null,
                () -> {
                    if (name != null && !name.isBlank()) {
                        target.setName(name);
                    }
                    if (type != null && !type.isBlank()) {
                        target.setType(type);
                    }
                    target.setUrl(url);
                    resourceRepository.update(target);
                    return target;
                }
        );
    }

    public void deleteResource(Long id) {
        validateNotNull(id, "Resource ID");
        Resource target = resourceRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Resource not found: " + id));

        executeWithTemplate(
                PermissionCodes.RESOURCE_DELETE,
                "DELETE_RESOURCE",
                target.getType(),
                id.toString(),
                null,
                () -> resourceRepository.delete(id)
        );
    }

    public List<Resource> listResources() {
        return executeWithTemplate(
                PermissionCodes.RESOURCE_LIST,
                "LIST_RESOURCES",
                "Resource",
                null,
                null,
                resourceRepository::findAll
        );
    }

    public Resource getResource(Long id) {
        validateNotNull(id, "Resource ID");
        Resource target = resourceRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Resource not found: " + id));

        return executeWithTemplate(
                PermissionCodes.RESOURCE_VIEW,
                "VIEW_RESOURCE",
                target.getType(),
                id.toString(),
                null,
                () -> target
        );
    }

    public ResourceAccessView getResourceAccess(Long id) {
        validateNotNull(id, "Resource ID");
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Resource not found: " + id));

        return executeWithTemplate(
                PermissionCodes.RESOURCE_VIEW,
                "VIEW_RESOURCE_ACCESS",
                resource.getType(),
                id.toString(),
                null,
                () -> {
                    List<ResourceRoleScope> roleScopes = permissionRepository.findRoleScopesByResource(id);
                    List<ResourceUserScope> userScopes = permissionRepository.findUserScopesByResource(id);
                    ResourceAccessView view = new ResourceAccessView();
                    view.setRoleScopes(roleScopes);
                    view.setUserScopes(userScopes);
                    return view;
                }
        );
    }

    /**
     * List resources that current user has scoped permissions for.
     * No permission required - users can always see their own scoped resources.
     */
    public List<Resource> listMyResources() {
        if (!sessionContext.isLoggedIn()) {
            throw new com.study.exception.PermissionDeniedException("Not logged in");
        }
        User currentUser = sessionContext.getCurrentUser();
        
        // Get scoped permissions from session context
        List<ScopedPermission> scopedPermissions = sessionContext.getScopedPermissions();
        if (scopedPermissions == null || scopedPermissions.isEmpty()) {
            return Collections.emptyList();
        }

        // Extract resource IDs from scoped permissions (resourceId is String, need to parse to Long)
        Set<Long> resourceIds = scopedPermissions.stream()
                .map(ScopedPermission::getResourceId)
                .filter(Objects::nonNull)
                .filter(id -> !id.isBlank())
                .map(id -> {
                    try {
                        return Long.parseLong(id);
                    } catch (NumberFormatException e) {
                        logger.warn("Invalid resource ID format: {}", id);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        
        if (resourceIds.isEmpty()) {
            return Collections.emptyList();
        }
        
        // Fetch resources by IDs
        logger.info("User {} listing {} scoped resources", currentUser.getUsername(), resourceIds.size());
        return resourceRepository.findByIds(resourceIds);
    }
}

package com.study.service;

import com.study.config.PermissionCodes;
import com.study.context.SessionContext;
import com.study.domain.Resource;
import com.study.exception.DataNotFoundException;
import com.study.repository.AuditLogRepository;
import com.study.repository.ResourceRepository;

import java.util.List;

/**
 * Service for managing resources.
 */
public class ResourceService extends BaseService {
    private final ResourceRepository resourceRepository;

    public ResourceService(SessionContext sessionContext,
                           ResourceRepository resourceRepository,
                           AuditLogRepository auditLogRepository) {
        super(sessionContext, auditLogRepository);
        this.resourceRepository = resourceRepository;
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
                .orElseThrow(() -> new DataNotFoundException("Resource not found: " + id));

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
                .orElseThrow(() -> new DataNotFoundException("Resource not found: " + id));

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
                .orElseThrow(() -> new DataNotFoundException("Resource not found: " + id));

        return executeWithTemplate(
                PermissionCodes.RESOURCE_VIEW,
                "VIEW_RESOURCE",
                target.getType(),
                id.toString(),
                null,
                () -> target
        );
    }
}

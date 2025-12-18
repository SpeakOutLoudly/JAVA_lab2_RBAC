package com.study.service;

import com.study.config.PermissionCodes;
import com.study.context.SessionContext;
import com.study.domain.AuditLog;
import com.study.domain.Resource;
import com.study.domain.Role;
import com.study.domain.User;
import com.study.exception.DataAccessException;
import com.study.exception.PermissionDeniedException;
import com.study.repository.*;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RbacIntegrationTest {
    private UserRepository userRepository;
    private RoleRepository roleRepository;
    private PermissionRepository permissionRepository;
    private ResourceRepository resourceRepository;
    private AuditLogRepository auditLogRepository;
    private SessionContext sessionContext;
    private AuthService authService;
    private UserService userService;
    private RoleService roleService;
    private PermissionService permissionService;
    private ResourceService resourceService;
    private AuditService auditService;

    @BeforeEach
    void setup() {
        DatabaseConnection.reset();
        System.setProperty("rbac.db.url", "jdbc:h2:mem:rbacTest;MODE=MySQL;DB_CLOSE_DELAY=-1");
        DatabaseConnection dbConnection = DatabaseConnection.getInstance();
        dbConnection.initializeDefaults();

        userRepository = new UserRepository(dbConnection);
        roleRepository = new RoleRepository(dbConnection);
        permissionRepository = new PermissionRepository(dbConnection);
        resourceRepository = new ResourceRepository(dbConnection);
        auditLogRepository = new AuditLogRepository(dbConnection);

        sessionContext = new SessionContext();
        authService = new AuthService(sessionContext, userRepository, permissionRepository, auditLogRepository);
        userService = new UserService(sessionContext, userRepository, roleRepository, auditLogRepository);
        roleService = new RoleService(sessionContext, roleRepository, permissionRepository, auditLogRepository);
        permissionService = new PermissionService(sessionContext, permissionRepository, auditLogRepository);
        resourceService = new ResourceService(sessionContext, resourceRepository, auditLogRepository);
        auditService = new AuditService(sessionContext, auditLogRepository);
    }

    @AfterEach
    void cleanup() {
        authService.logout();
        DatabaseConnection.reset();
        System.clearProperty("rbac.db.url");
    }

    @Test
    void createUserRollsBackWhenRoleAssignmentFails() {
        authService.login("admin", "admin123");
        long invalidRoleId = 9999L;

        assertThrows(DataAccessException.class,
                () -> userService.createUser("tx-user", "secret123", invalidRoleId));

        assertTrue(userRepository.findByUsername("tx-user").isEmpty(),
                "User should not be persisted when role assignment fails");
    }

    @Test
    void deletingRoleRequiresProperPermission() {
        authService.login("admin", "admin123");
        Role userRole = roleRepository.findByCode("USER").orElseThrow();
        Long userRoleId = userRole.getId();

        User limitedUser = userService.createUser("limited", "pwd123", userRoleId);
        authService.logout();

        authService.login(limitedUser.getUsername(), "pwd123");
        assertThrows(PermissionDeniedException.class,
                () -> roleService.deleteRole(userRoleId),
                "Non-admin should not be able to delete roles");
    }

    @Test
    void scopedPermissionRestrictsResourceAccess() {
        authService.login("admin", "admin123");
        Resource project1 = resourceService.createResource("PRJ-1", "Project One", "PROJECT", null);
        Resource project2 = resourceService.createResource("PRJ-2", "Project Two", "PROJECT", null);

        Role scopedRole = roleService.createRole("PROJECT_EDITOR", "Project Editor", null);
        permissionService.assignScopedPermissionToRole(
                scopedRole.getId(),
                PermissionCodes.RESOURCE_UPDATE,
                "PROJECT",
                project1.getId().toString()
        );

        User scopedUser = userService.createUser("scoped-user", "pwd12345", scopedRole.getId());
        authService.logout();

        authService.login("scoped-user", "pwd12345");
        resourceService.updateResource(project1.getId(), "Renamed", null, null);
        assertThrows(PermissionDeniedException.class,
                () -> resourceService.updateResource(project2.getId(), "Should Fail", null, null));
    }

    @Test
    void auditQueriesFilterByActionAndResource() {
        authService.login("admin", "admin123");
        resourceService.listResources();
        Resource resource = resourceService.createResource("PRJ-3", "Project Three", "PROJECT", null);

        List<AuditLog> byAction = auditService.viewAuditLogsByAction("LIST_RESOURCES", 10);
        assertFalse(byAction.isEmpty(), "Action query should return audit logs");

        List<AuditLog> byResource = auditService.viewAuditLogsByResource("PROJECT", resource.getCode(), 10);
        assertFalse(byResource.isEmpty(), "Resource query should return audit logs");
    }
}

# RBAC CLI System - 系统架构文档

## 1. 系统架构图

### 1.1 整体分层架构
```
┌─────────────────────────────────────────────────────────────┐
│                     CLI Layer (表现层)                       │
│  ┌──────────────┐              ┌──────────────────────┐     │
│  │ GuestState   │◄────────────►│ LoggedInState        │     │
│  │ (登录前菜单)  │   状态切换    │ (登录后菜单)          │     │
│  └──────────────┘              └──────────────────────┘     │
│         │                              │                     │
└─────────┼──────────────────────────────┼─────────────────────┘
          │                              │
          └──────────────┬───────────────┘
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                   Facade Layer (外观层)                      │
│              ┌─────────────────────┐                        │
│              │   RbacFacade        │                        │
│              │  - login()          │                        │
│              │  - createUser()     │                        │
│              │  - assignRole()     │                        │
│              └─────────────────────┘                        │
│                       │                                      │
└───────────────────────┼──────────────────────────────────────┘
                        │
        ┌───────────────┼───────────────┐
        ▼               ▼               ▼
┌─────────────────────────────────────────────────────────────┐
│                  Service Layer (业务层)                      │
│  ┌──────────┐   ┌──────────┐   ┌──────────┐                │
│  │  Auth    │   │  User    │   │Bootstrap │                │
│  │ Service  │   │ Service  │   │ Service  │                │
│  └────┬─────┘   └────┬─────┘   └────┬─────┘                │
│       │              │              │                       │
│       └──────────────┴──────────────┘                       │
│                      │                                      │
│              ┌───────▼───────┐                              │
│              │ BaseService   │ ◄─ 统一模板:                 │
│              │ - 鉴权        │    1. Authorization          │
│              │ - 校验        │    2. Validation             │
│              │ - 执行        │    3. Execution              │
│              │ - 审计        │    4. Audit                  │
│              └───────────────┘                              │
└─────────────────────────────────────────────────────────────┘
                        │
        ┌───────────────┼───────────────┬──────────┐
        ▼               ▼               ▼          ▼
┌─────────────────────────────────────────────────────────────┐
│                Repository Layer (持久层)                     │
│  ┌────────┐  ┌────────┐  ┌──────────┐  ┌──────────┐        │
│  │  User  │  │  Role  │  │Permission│  │ AuditLog │        │
│  │  Repo  │  │  Repo  │  │   Repo   │  │   Repo   │        │
│  └────┬───┘  └────┬───┘  └────┬─────┘  └────┬─────┘        │
│       │           │           │             │               │
│       └───────────┴───────────┴─────────────┘               │
│                   │                                         │
│           ┌───────▼────────┐                                │
│           │ BaseRepository │ ◄─ 事务管理                    │
│           │ - Transaction  │                                │
│           │ - Connection   │                                │
│           └────────────────┘                                │
└─────────────────────────────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────┐
│                    Database (H2)                            │
│  ┌────────┐  ┌────────┐  ┌───────────┐  ┌──────────────┐  │
│  │ users  │  │ roles  │  │permissions│  │ audit_logs   │  │
│  └────────┘  └────────┘  └───────────┘  └──────────────┘  │
│  ┌──────────┐           ┌─────────────┐                    │
│  │user_roles│           │role_perms   │                    │
│  └──────────┘           └─────────────┘                    │
└─────────────────────────────────────────────────────────────┘
```

## 2. 核心流程图

### 2.1 登录流程
```
User Input (username/password)
        │
        ▼
┌──────────────────────┐
│  GuestMenuState      │
│  handleLogin()       │
└──────┬───────────────┘
       │
       ▼
┌──────────────────────┐
│  RbacFacade          │
│  login()             │
└──────┬───────────────┘
       │
       ▼
┌──────────────────────────────────────────┐
│  AuthService.login()                     │
│  ┌────────────────────────────────────┐  │
│  │ 1. Validation (username/password)  │  │
│  │ 2. UserRepo.findByUsername()       │  │
│  │ 3. PasswordEncoder.matches()       │  │
│  │ 4. PermissionRepo.findByUserId()   │  │
│  │ 5. SessionContext.setUser()        │  │
│  │ 6. SessionContext.cachePermissions()│ │
│  │ 7. AuditRepo.save(LOGIN_SUCCESS)   │  │
│  └────────────────────────────────────┘  │
└──────┬───────────────────────────────────┘
       │
       ▼
┌──────────────────────┐
│ State Switch:        │
│ GuestState →         │
│ LoggedInState        │
└──────────────────────┘
```

### 2.2 创建用户+分配角色流程 (事务)
```
Command: create-user
        │
        ▼
┌─────────────────────────────────────────────────┐
│ UserService.createUser()                        │
│                                                 │
│  executeWithTemplate(                           │
│    permission: USER_CREATE,                     │
│    validation: validateInputs(),                │
│    execution: () -> {                           │
│      ┌──────────────────────────────────┐      │
│      │  START TRANSACTION               │      │
│      │  ┌────────────────────────────┐  │      │
│      │  │ 1. Generate salt           │  │      │
│      │  │ 2. Hash password           │  │      │
│      │  │ 3. UserRepo.save()         │  │      │
│      │  │ 4. RoleRepo.assignRole()   │  │      │
│      │  │ 5. AuditRepo.save()        │  │      │
│      │  └────────────────────────────┘  │      │
│      │  COMMIT / ROLLBACK              │      │
│      └──────────────────────────────────┘      │
│    }                                            │
│  )                                              │
└─────────────────────────────────────────────────┘
```

### 2.3 权限检查流程
```
Command Execution
        │
        ▼
┌───────────────────────────────────┐
│ BaseService.executeWithTemplate() │
│                                   │
│  Step 1: Authorization            │
│  ┌─────────────────────────────┐  │
│  │ checkPermission()           │  │
│  │ ├─ isLoggedIn()?           │  │
│  │ │   No → PermissionDenied  │  │
│  │ │                          │  │
│  │ └─ SessionContext          │  │
│  │     .hasPermission(code)?  │  │
│  │     No → PermissionDenied  │  │
│  │     Yes → Continue         │  │
│  └─────────────────────────────┘  │
│                                   │
│  Step 2: Validation               │
│  ┌─────────────────────────────┐  │
│  │ validateNotNull()           │  │
│  │ validateNotBlank()          │  │
│  │ Custom validations          │  │
│  └─────────────────────────────┘  │
│                                   │
│  Step 3: Execution                │
│  ┌─────────────────────────────┐  │
│  │ Business logic              │  │
│  │ Repository operations       │  │
│  └─────────────────────────────┘  │
│                                   │
│  Step 4: Audit                    │
│  ┌─────────────────────────────┐  │
│  │ Success → auditSuccess()    │  │
│  │ Failure → auditFailure()    │  │
│  └─────────────────────────────┘  │
└───────────────────────────────────┘
```

## 3. 数据库E-R图
```
┌──────────────┐           ┌──────────────┐
│    users     │           │    roles     │
├──────────────┤           ├──────────────┤
│ id (PK)      │           │ id (PK)      │
│ username     │           │ code         │
│ password_hash│           │ name         │
│ salt         │           │ description  │
│ enabled      │           │ created_at   │
│ created_at   │           └──────┬───────┘
│ updated_at   │                  │
└──────┬───────┘                  │
       │                          │
       │         ┌────────────────┘
       │         │
       │         │
┌──────▼─────────▼─────┐
│    user_roles        │
├──────────────────────┤
│ user_id (FK, PK)     │
│ role_id (FK, PK)     │
│ created_at           │
└──────────────────────┘


┌──────────────┐           ┌──────────────┐
│    roles     │           │ permissions  │
├──────────────┤           ├──────────────┤
│ id (PK)      │           │ id (PK)      │
│ code         │           │ code         │
│ name         │           │ name         │
│ description  │           │ description  │
│ created_at   │           │ resource_id  │
└──────┬───────┘           │ created_at   │
       │                   └──────┬───────┘
       │                          │
       │         ┌────────────────┘
       │         │
       │         │
┌──────▼─────────▼─────┐
│  role_permissions    │
├──────────────────────┤
│ role_id (FK, PK)     │
│ permission_id (FK,PK)│
│ created_at           │
└──────────────────────┘


┌──────────────┐
│ audit_logs   │
├──────────────┤
│ id (PK)      │
│ user_id      │
│ username     │
│ action       │
│ resource_type│
│ resource_id  │
│ detail       │
│ success      │
│ error_message│
│ ip_address   │
│ created_at   │
└──────────────┘
```

## 4. 状态机图
```
┌─────────────────┐
│                 │  login success
│  GuestState     ├──────────────┐
│  (未登录)        │              │
│                 │◄─────────┐   │
└─────────────────┘          │   │
                             │   │
                          logout │
                             │   │
                             │   ▼
                  ┌──────────┴────────────┐
                  │                       │
                  │  LoggedInState        │
                  │  (已登录)              │
                  │                       │
                  │  - 显示可用命令        │
                  │  - 权限过滤           │
                  │  - 命令路由           │
                  │                       │
                  └───────────────────────┘
```

## 5. 命令-权限映射
```
CommandSpec 枚举结构:
┌─────────────────────┬───────────────────┬──────────────┐
│ Command             │ Description       │ Permission   │
├─────────────────────┼───────────────────┼──────────────┤
│ login               │ User login        │ null         │
│ logout              │ Logout            │ null         │
│ create-user         │ Create new user   │ USER_CREATE  │
│ list-users          │ List all users    │ USER_LIST    │
│ view-user           │ View user details │ USER_VIEW    │
│ assign-role         │ Assign role       │ ROLE_ASSIGN  │
│ view-profile        │ View own profile  │ null         │
│ change-password     │ Change password   │ null         │
│ ...                 │ ...               │ ...          │
└─────────────────────┴───────────────────┴──────────────┘

Menu显示逻辑:
for each CommandSpec:
    if permission == null OR hasPermission(permission):
        显示命令
    else:
        隐藏命令
```

## 6. 异常处理流程
```
User Command
     │
     ▼
try {
    executeWithTemplate()
} catch (PermissionDeniedException e) {
    │
    ├─ auditFailure(permission_denied)
    ├─ logger.warn()
    └─ Display: "❌ Permission denied"
} catch (ValidationException e) {
    │
    ├─ auditFailure(validation_failed)
    ├─ logger.warn()
    └─ Display: "❌ Invalid input"
} catch (DataNotFoundException e) {
    │
    ├─ auditFailure(data_not_found)
    ├─ logger.warn()
    └─ Display: "❌ Not found"
} catch (DuplicateDataException e) {
    │
    ├─ auditFailure(duplicate_data)
    ├─ logger.warn()
    └─ Display: "❌ Already exists"
} catch (PersistenceException e) {
    │
    ├─ auditFailure(database_error)
    ├─ logger.error(stacktrace)
    └─ Display: "❌ System error"
}
```

## 7. 核心设计模式应用

### 7.1 外观模式 (Facade)
```
CLI → RbacFacade → { AuthService, UserService, ... }
     (简化接口)     (复杂子系统)
```

### 7.2 状态模式 (State)
```
MenuState接口
├─ GuestMenuState (登录前)
└─ LoggedInMenuState (登录后)
```

### 7.3 模板方法 (Template Method)
```
BaseService.executeWithTemplate()
├─ checkPermission()    (hook)
├─ validation()         (hook)
├─ execution()          (abstract)
└─ audit()              (hook)
```

### 7.4 工厂模式 (Factory)
```
PasswordEncoder接口
└─ Sha256PasswordEncoder实现
   (未来可扩展: BCryptPasswordEncoder等)
```

## 8. 关键设计决策

### 8.1 权限缓存策略
- **缓存位置**: SessionContext (内存)
- **缓存时机**: 登录时
- **刷新时机**: 角色/权限变更时
- **好处**: 避免每次操作查库,提升性能

### 8.2 事务边界
- **原则**: 组合操作必须有事务
- **示例**: 创建用户 + 分配角色 + 审计
- **实现**: BaseRepository.executeInTransaction()

### 8.3 审计策略
- **审计时机**: 所有业务操作(成功/失败)
- **审计内容**: 用户、操作、资源、结果、时间
- **审计存储**: 数据库(audit_logs表)
- **日志分离**: 审计入库 + 运行日志文件

### 8.4 密码安全
- **算法**: SHA-256
- **加固**: 随机salt (每用户独立)
- **存储**: 只存hash + salt,明文不落地
- **验证**: hash(input + salt) == stored_hash

## 9. 性能优化点

1. **权限缓存** - 减少数据库查询
2. **连接池** - DatabaseConnection单例
3. **PreparedStatement** - 防SQL注入+性能
4. **索引** - username, code等常查字段
5. **事务批处理** - 组合操作一次提交

## 10. 扩展性设计

### 10.1 水平扩展
- Repository接口化 → 可切换数据源(MySQL/PostgreSQL)
- PasswordEncoder接口化 → 可切换加密算法
- CommandSpec枚举化 → 易于添加新命令

### 10.2 垂直扩展
- BaseService模板 → 新Service继承即可
- 审计钩子 → 统一注入,无需每处手写
- 异常体系 → 新异常类型易添加

# RBAC-cli# RBAC CLI System

基于Java的完整RBAC(Role-Based Access Control)命令行系统

## 🎯 项目简介

这是一个生产级别的基于角色的访问控制系统,采用命令行交互方式。系统完整实现了用户管理、角色管理、权限管理、审计日志等核心功能,代码架构清晰,安全可靠,易于扩展。

## ✨ 核心特性

- ✅ **完整的RBAC模型** - User-Role-Permission三层映射
- ✅ **统一服务模板** - 鉴权→校验→执行→审计的标准化流程
- ✅ **权限缓存机制** - 登录时缓存有效权限,提升性能
- ✅ **完善的审计日志** - 所有操作自动审计,成功失败都记录
- ✅ **事务管理** - 组合操作原子性保证,失败自动回滚
- ✅ **密码安全** - SHA-256 + 随机salt,永不存储明文
- ✅ **数据脱敏** - 日志中敏感信息自动脱敏
- ✅ **命令权限绑定** - 声明式权限配置,易于扩展
- ✅ **状态模式CLI** - 登录前后菜单自动切换
- ✅ **异常统一处理** - 完整的异常体系,友好的错误提示

## 🏗️ 架构设计

### 分层架构
```
CLI层 (表现层)
    ↓
Facade层 (外观层)
    ↓
Service层 (业务层)
    ↓
Repository层 (持久层)
    ↓
Database层 (H2)
```

### 核心设计模式
- **外观模式** - 简化CLI与多个Service的交互
- **状态模式** - 管理登录前后的菜单状态
- **模板方法** - 统一业务执行流程
- **工厂模式** - 支持多种密码加密算法
- **单例模式** - 数据库连接管理

## 🚀 快速开始

### 前置要求
- Java 23+
- Maven 3.6+
- MySQL 8.0+ (本地运行在localhost:3306，需要创建数据库`campus_trade`)

### 安装运行

#### 方式1: 使用启动脚本(推荐)
```bash
# Windows
start.bat

# Linux/Mac
chmod +x start.sh
./start.sh
```

#### 方式2: Maven命令
```bash
# 编译
mvn clean compile

# 运行
mvn exec:java -Dexec.mainClass="com.study.Main"
```

### 默认账号
首次运行系统会自动初始化,创建默认管理员账号:
- **用户名**: `admin`
- **密码**: `admin123`

## 📚 使用示例

### 登录系统
```
Enter command: login
Username: admin
Password: admin123

✓ Login successful! Welcome, admin
```

### 创建用户
```
Enter command: create-user
New username: john
Password: john123
Role code (optional): USER

✓ User created: john
```

### 分配角色
```
Enter command: assign-role
Username: john
Role code: ADMIN

✓ Role assigned successfully
```

### 查看用户列表
```
Enter command: list-users

─── Users (2) ───
[1] admin - Active
[2] john - Active
```

## 📖 文档

- **[IMPLEMENTATION.md](IMPLEMENTATION.md)** - 详细实现文档
  - 核心特性说明(分层架构、统一服务模板、权限缓存、事务管理等)
  - 目录结构详解
  - 数据库设计
  - 核心流程说明
  - 设计模式应用
  - 扩展点指南
  - 已实现的11个改进建议清单
  
- **[ARCHITECTURE.md](ARCHITECTURE.md)** - 完整架构文档
  - 整体分层架构图
  - 核心流程图(登录、创建用户、权限检查)
  - 数据库E-R图
  - 状态机图
  - 命令-权限映射表
  - 异常处理流程
  - 设计模式应用详解
  - 关键设计决策说明
  - 性能优化点
  
- **[SUMMARY.md](SUMMARY.md)** - 项目总结文档
  - 11项改进建议完成情况详解
  - 代码统计(约27个核心类,2600+行代码)
  - 技术栈与设计模式总结
  - 运行说明
  - 完整文件清单
  - 核心亮点分析
  - 后续扩展建议(短期/中期/长期)

## 🗂️ 项目结构

```
src/main/java/com/study/
├── cli/              # CLI层
│   ├── handler/      # 命令处理器(按领域分类)
│   ├── state/        # 菜单状态(登录前/后)
│   └── CliApplication.java  # CLI应用主类
├── common/           # 通用工具
│   └── util/         # 工具类(输入处理等)
├── config/           # 配置类(命令规格、权限代码)
├── context/          # 上下文(会话、权限缓存)
├── domain/           # 领域实体(User, Role, Permission, Resource, ScopedPermission等)
├── exception/        # 异常体系
├── facade/           # 外观层
├── repository/       # 持久层
├── security/         # 安全组件(密码加密、数据脱敏)
├── service/          # 业务服务层
└── Main.java         # 程序入口
```

## 🔒 安全特性

### 密码安全
- SHA-256哈希算法
- 每用户独立随机salt
- 明文密码永不存储和记录

### 权限控制
- 细粒度权限管理
- 登录时缓存有效权限集
- 每个操作自动鉴权

### 审计日志
- 所有敏感操作强制审计
- 记录操作者、动作、资源、结果、时间
- 成功和失败都记录

### 数据脱敏
- 密码完全屏蔽
- 用户名部分遮蔽
- 日志中不暴露敏感信息

## 🎨 功能列表

### 用户管理
- [x] 创建用户
- [x] 查看用户列表
- [x] 查看用户详情
- [x] 修改用户(启用/禁用)
- [x] 删除用户
- [x] 分配角色给用户
- [x] 移除用户角色

### 角色管理
- [x] 创建角色
- [x] 查看角色列表
- [x] 修改角色
- [x] 删除角色

### 权限管理
- [x] 创建权限
- [x] 查看权限列表
- [x] 查看我的权限
- [x] 修改权限
- [x] 删除权限
- [x] 分配权限给角色
- [x] 移除角色权限
- [x] 分配资源权限(范围控制)
- [x] 移除资源权限

### 资源管理
- [x] 创建资源
- [x] 查看资源列表
- [x] 查看资源详情
- [x] 修改资源
- [x] 删除资源

### 审计日志
- [x] 查看自己的操作日志
- [x] 查看所有审计日志(需权限)
- [x] 按用户查询日志
- [x] 按操作类型查询
- [x] 按资源查询日志

### 系统功能
- [x] 用户登录/登出
- [x] 查看个人资料
- [x] 修改密码
- [x] 自动初始化默认数据
- [x] 层级菜单导航(数字选择)
- [x] 权限驱动菜单显示

## 🛠️ 技术栈

- **语言**: Java 23
- **构建工具**: Maven
- **数据库**: MySQL 8.0
- **日志框架**: SLF4J + Logback
- **测试框架**: JUnit 5
- **设计模式**: 外观、状态、模板方法、策略、命令、单例

## 📊 数据库设计

### 核心表
- `users` - 用户表
- `roles` - 角色表
- `permissions` - 权限表
- `resources` - 资源表
- `user_roles` - 用户-角色关联
- `role_permissions` - 角色-权限关联
- `role_permission_scopes` - 角色资源权限范围表
- `audit_logs` - 审计日志

### 关系
```
User ←→ Role ←→ Permission → Resource
                     ↓
                 AuditLog
```

## 🧪 测试

测试框架已集成,可直接编写单元测试和集成测试:

```bash
# 运行测试
mvn test
```

## 🔧 扩展开发

### 添加新命令
1. 在`PermissionCodes`添加权限常量
2. 在`CommandSpec`枚举添加命令定义
3. 在`DatabaseConnection.initializeDefaults`初始化权限和角色映射
4. 在对应的`CommandHandler`中实现命令处理逻辑
5. 在`LoggedInMenuState`的子菜单中添加菜单项
6. 在`RbacFacade`中添加对应的Facade方法(如需要)

### 切换数据库
1. 修改`DatabaseConnection`中的`DEFAULT_DB_URL`、`DB_USER`、`DB_PASSWORD`常量
2. 在`pom.xml`中添加对应数据库驱动依赖
3. 调整SQL语法(如需要，当前使用MySQL)

### 更换加密算法
1. 实现`PasswordEncoder`接口
2. 在`AuthService`和`UserService`中替换`Sha256PasswordEncoder`为新实现

## 📝 许可证

MIT License

## 👥 作者

RBAC CLI System Development Team

## 🙏 致谢

感谢所有为改进建议提供反馈的开发者。

---

**⭐ 如果这个项目对你有帮助,请给个Star!**

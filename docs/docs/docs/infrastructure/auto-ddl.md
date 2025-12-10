---
sidebar_position: 7
---

# MyBatis-Plus Auto DDL

本项目使用MyBatis-Plus的Auto DDL功能实现数据库自动初始化和版本管理。

## 🎯 什么是Auto DDL

**Auto DDL** (Automatic DDL Execution) 是MyBatis-Plus提供的数据库脚本自动执行功能，类似于Flyway/Liquibase，但更轻量级。

### 核心特性

- ✅ **自动扫描**: 启动时扫描指定目录的SQL脚本
- ✅ **版本管理**: 基于文件名的版本号排序执行
- ✅ **执行历史**: 记录在`ddl_history`表中
- ✅ **事务保护**: 整个DDL过程在一个事务中执行
- ✅ **幂等性**: 重复启动不会重复执行已执行的脚本

## 📁 项目实现

### 目录结构

```
blog-application/
├── src/main/java/com/blog/config/ddl/
│   ├── AutoDdlInitializer.java     # DDL初始化器（事务管理）
│   ├── DdlProperties.java          # 配置属性类（@ConfigurationProperties）
│   └── DdlScriptManager.java       # 脚本扫描和管理
│
└── src/main/resources/
    ├── application-dev.yaml         # 开发环境配置
    └── db/                          # DDL脚本目录
        ├── README.md
        ├── V0.0.1__create_database.sql
        ├── V1.0.0__init_schema.sql
        └── V1.0.1__init_system_data.sql
```

### 核心组件

#### 1. DdlProperties (配置属性类)

**位置**: `com.blog.config.ddl.DdlProperties`

**职责**:
- 封装Auto DDL所有配置项
- 使用`@ConfigurationProperties`实现类型安全的配置绑定
- 提供配置验证和默认值

**实现示例**:

```java
@Data
@Configuration
@ConfigurationProperties(prefix = "mybatis-plus.auto-ddl")
public class DdlProperties {
    
    /** 是否启用Auto DDL功能 */
    private boolean enabled = false;
    
    /** DDL脚本目录路径（相对于classpath） */
    private String scriptDir = "db/";
    
    /** 获取脚本扫描路径模式 */
    public String getScriptPattern() {
        String dir = scriptDir.endsWith("/") ? scriptDir : scriptDir + "/";
        return "classpath*:" + dir + "**/*.sql";
    }
}
```

**优势**:
- ✅ IDE自动补全支持
- ✅ 编译时类型检查
- ✅ 统一配置管理
- ✅ 符合Spring Boot最佳实践

#### 2. AutoDdlInitializer (初始化器)

**位置**: `com.blog.config.ddl.AutoDdlInitializer`

**职责**:
- 在应用启动时自动执行DDL脚本
- 提供事务管理能力
- 控制DDL执行优先级

**关键注解**:

```java
@Configuration
@Order(0)                           // 最高优先级，最先执行
@Profile("!test")                   // 测试环境不启用
@ConditionalOnProperty(
    name = "mybatis-plus.auto-ddl.enabled",
    havingValue = "true",
    matchIfMissing = false          // 默认禁用
)
@Transactional                      // 事务保护
public class AutoDdlInitializer implements ApplicationRunner
```

#### 3. DdlScriptManager (脚本管理器)

**位置**: `com.blog.config.ddl.DdlScriptManager`

**职责**:
- 扫描DDL脚本目录下的所有SQL文件
- 按版本号自然排序
- 提供脚本列表给MyBatis-Plus执行
- **性能优化**: 使用缓存避免重复扫描文件系统

**依赖注入与缓存**:

```java
@Component
public class DdlScriptManager implements IDdl {
    
    private final DataSource dataSource;
    private final ApplicationContext applicationContext;
    private final DdlProperties ddlProperties;  // 注入配置类
    
    // 缓存字段 - 避免重复扫描文件系统
    /**
     * 缓存的DDL脚本路径列表
     * <p>
     * MyBatis-Plus框架会多次调用getSqlFiles()方法，
     * 使用缓存避免重复扫描文件系统，提升性能并减少日志噪音。
     */
    private List<String> cachedScriptPaths = null;
    
    public DdlScriptManager(DataSource dataSource,
                           ApplicationContext applicationContext,
                           DdlProperties ddlProperties) {
        // 构造器注入
    }
}
```

**缓存机制说明**:

> 💡 **性能优化**: 在优化前，MyBatis-Plus框架会调用 `getSqlFiles()` 方法**6次**，导致重复扫描文件系统。
> 通过添加缓存机制，现在只在首次调用时扫描，后续调用直接返回缓存结果，
> 大幅减少启动时间并降低日志噪音。

**扫描模式**:

```java
// 从DdlProperties获取配置
String pattern = ddlProperties.getScriptPattern();
// 例如: "classpath*:db/**/*.sql"

匹配示例:
✅ db/V0.0.1__create_database.sql
✅ db/V1.0.0__init_schema.sql  
✅ db/v2/V1.0.2__add_indexes.sql
❌ db/backup/old_script.sql.bak
```

## ⚙️ 配置说明

### application-dev.yaml

```yaml
# MyBatis-Plus Auto DDL 配置
mybatis-plus:
  auto-ddl:
    enabled: true      # ✅ 开发环境：启用
    script-dir: db/    # DDL脚本目录（相对于classpath）
```

### application-prod.yaml

```yaml
mybatis-plus:
  auto-ddl:
    enabled: false     # ❌ 生产环境：禁用
```

### 环境建议

| 环境 | enabled | 说明 |
|------|---------|------|
| **开发** | `true` | ✅ 方便快速迭代和数据库同步 |
| **测试** | `true` | ✅ 自动初始化测试数据库 |
| **生产** | `false` | ❌ 使用专业工具（Flyway）管理 |

## 📝 脚本命名规范

### 标准格式

```
V<major>.<minor>.<patch>__<description>.sql

示例:
V1.0.0__init_schema.sql
V1.0.1__init_system_data.sql
V1.0.2__add_user_indexes.sql
```

### 命名规则

- **V**: 固定前缀（大写V）
- **版本号**: 三段式版本号 `major.minor.patch`
- **双下划线**: `__` 分隔版本号和描述
- **描述**: 使用小写和下划线，简洁明了
- **扩展名**: `.sql`

### 版本排序

脚本按**自然排序**执行：

```
V0.0.1__create_database.sql      # 第1个执行
V1.0.0__init_schema.sql          # 第2个执行
V1.0.1__init_system_data.sql     # 第3个执行
V1.0.2__add_indexes.sql          # 第4个执行
V2.0.0__major_refactor.sql       # 第5个执行
```

## 🚀 实际执行流程

### 启动日志示例

```log
2025-12-10 19:33:34.151 INFO  [restartedMain] AutoDdlInitializer: 
    📋 Found 1 IDdl implementation(s). Starting transactional DDL execution...

2025-12-10 19:33:34.151 INFO  [restartedMain] AutoDdlInitializer:
    ├─ Using IDdl bean: com.blog.config.ddl.DdlScriptManager

2025-12-10 19:33:34.152 INFO  [restartedMain] DdlScriptManager:
    🔍 Scanning for DDL scripts with pattern: classpath*:db/**/*.sql

2025-12-10 19:33:34.153 INFO  [restartedMain] DdlScriptManager:
    📋 DDL Execution Plan (3 scripts):
    ├─ db/V0.0.1__create_database.sql
    ├─ db/V1.0.0__init_schema.sql
    ├─ db/V1.0.1__init_system_data.sql

2025-12-10 19:33:34.181 INFO  [restartedMain] AutoDdlInitializer:
    ✅ Transactional DDL execution finished successfully.
```

### 执行历史记录

所有脚本执行记录保存在`ddl_history`表：

| script | type | version | 说明 |
|--------|------|---------|------|
| db/V0.0.1__create_database.sql | sql | 202512101931 | 创建数据库 |
| db/V1.0.0__init_schema.sql | sql | 202512101931 | 初始化表结构 |
| db/V1.0.1__init_system_data.sql | sql | 202512101931 | 初始化系统数据 |

> **version字段**: 执行时间戳（yyyyMMddHHmm格式）

## 📚 DDL脚本示例

### V0.0.1__create_database.sql

```sql
-- 创建数据库
CREATE DATABASE IF NOT EXISTS blog_db
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE blog_db;
```

### V1.0.0__init_schema.sql

```sql
USE blog_db;

-- 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码（BCrypt加密）',
    email VARCHAR(100) UNIQUE COMMENT '邮箱',
    status TINYINT DEFAULT 1 COMMENT '状态 0=禁用 1=启用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 创建索引
CREATE INDEX idx_username ON sys_user(username);
CREATE INDEX idx_status ON sys_user(status);
```

### V1.0.1__init_system_data.sql

```sql
USE blog_db;

-- 插入默认角色
INSERT IGNORE INTO sys_role (id, role_name, role_key, status) VALUES
(1, '管理员', 'ADMIN', 1),
(2, '作者', 'AUTHOR', 1),
(3, '普通用户', 'USER', 1);

-- 插入默认管理员（密码: Admin@123）
INSERT IGNORE INTO sys_user (id, username, password, email, status) VALUES
(1, 'admin', '$2a$10$...', 'admin@example.com', 1);
```

## 🔐 安全注意事项

### 1. 默认密码

脚本中包含默认管理员账户：

- 用户名: `admin`
- 密码: `Admin@123`

> ⚠️ **生产环境部署前必须修改默认密码！**

### 2. 生产环境

**不推荐**在生产环境启用Auto DDL，建议：

1. 使用专业迁移工具（Flyway/Liquibase）
2. 通过CI/CD流程执行数据库变更
3. 严格的变更审批流程

### 3. 脚本幂等性

所有脚本应确保幂等性：

```sql
-- ✅ 推荐：使用IF NOT EXISTS
CREATE TABLE IF NOT EXISTS sys_user (...);

-- ✅ 推荐：使用INSERT IGNORE
INSERT IGNORE INTO sys_role VALUES (...);

-- ❌ 避免：直接CREATE/INSERT
CREATE TABLE sys_user (...);   -- 重复执行会报错
INSERT INTO sys_role VALUES (...);  -- 可能违反唯一约束
```

## 🛠️ 常见操作

### 添加新脚本

1. **创建脚本文件**:
   ```bash
   touch blog-application/src/main/resources/db/V1.0.2__add_user_indexes.sql
   ```

2. **编写SQL**:
   ```sql
   USE blog_db;
   
   CREATE INDEX idx_email ON sys_user(email);
   CREATE INDEX idx_created_at ON sys_user(created_at);
   ```

3. **重启应用**: 脚本自动执行

### 禁用Auto DDL

修改配置文件：

```yaml
mybatis-plus:
  auto-ddl:
    enabled: false  # 禁用后不会执行任何DDL脚本
```

### 手动执行脚本

如果Auto DDL被禁用，可以手动执行：

```bash
mysql -u root -p blog_db < src/main/resources/db/V1.0.2__add_user_indexes.sql
```

## 🔍 故障排查

### 问题1: 脚本未执行

**症状**: 启动时没有DDL相关日志

**排查**:

```bash
# 1. 检查配置
grep "auto-ddl" application-dev.yaml

# 2. 检查Profile
# 确认启动时激活了dev profile
spring.profiles.active=dev

# 3. 查看日志
tail -f logs/personal-blog-backend.log | grep -i "ddl\|AutoDdl"
```

### 问题2: 脚本执行失败

**症状**: 应用启动失败，报SQL错误

**原因**: SQL语法错误或数据库权限不足

**解决**:

1. 查看错误日志
2. 检查SQL语法
3. 确认数据库用户权限
4. 手动执行SQL测试

### 问题3: 重复执行问题

**症状**: 脚本被重复执行

**原因**: `ddl_history`表被清空或损坏

**解决**:

```sql
-- 查看执行历史
SELECT * FROM ddl_history ORDER BY version DESC;

-- 如果历史丢失，需要重建数据库或手动插入记录
```

## 📊 性能优化建议

### 1. 大量数据初始化

对于大量初始数据，考虑：

```sql
-- 禁用索引检查（加速插入）
SET UNIQUE_CHECKS=0;
SET FOREIGN_KEY_CHECKS=0;

-- 批量插入
INSERT INTO sys_user VALUES
(1, 'user1', ...),
(2, 'user2', ...),
(3, 'user3', ...);

-- 恢复检查
SET UNIQUE_CHECKS=1;
SET FOREIGN_KEY_CHECKS=1;
```

### 2. 索引创建

在数据导入后再创建索引：

```sql
-- V1.0.0: 只创建表结构
CREATE TABLE sys_user (...);

-- V1.0.1: 导入数据
INSERT INTO sys_user VALUES (...);

-- V1.0.2: 创建索引
CREATE INDEX idx_username ON sys_user(username);
```

## 📚 相关文档

- [MyBatis-Plus官方文档](https://baomidou.com/)
- [开发规范](../development/standards.md)
- [数据库设计](../architecture/overview.md)

---

**文档维护**: 如有任何问题或建议，请提交Issue或PR。

# 📋 Personal Blog Backend - 项目开发规范

> **版本**: 2.0  
> **最后更新**: 2025-12-07  
> **维护人**: liusxml

本文档定义了项目的**强制性**规则、编码标准和架构约束，所有开发者（包括 AI 助手）都必须遵守。

---

## 1. 技术栈（不可变更）

### 核心技术
*   **语言**: Java 21
*   **框架**: Spring Boot 3.5.7
*   **ORM**: MyBatis-Plus 3.5.14
*   **数据库**: MySQL 9.4.0

### 开发工具
*   **Lombok**: 消除样板代码（`@Data`, `@RequiredArgsConstructor`, `@Slf4j`）
*   **MapStruct**: 高性能 Bean 映射（DTO ↔ Entity）
*   **Commons-Lang3 / Collections4 / Guava**: 工具函数库
*   **SpringDoc**: OpenAPI/Swagger 文档生成

### 基础设施
*   **Redis**: 分布式缓存和会话管理
*   **Spring Security**: 认证和授权
*   **Flyway**: 数据库版本控制
*   **Spring Boot Actuator**: 健康监控

---

## 2. 架构硬性规则（模块化单体）

### 2.1 模块结构
*   **`*-api` 模块**: 仅包含 DTOs、接口、枚举。**严禁**包含实体类和业务逻辑。
*   **`*-service` 模块**: 包含 Controllers、Services、Entities、Mappers。
*   **`blog-application` 模块**: 仅包含启动类和全局配置。**严禁**包含业务代码。

### 2.2 依赖规则
*   ✅ **允许**: `Service` → `API`
*   ❌ **严禁**: `Service` → `Service`（跨模块直接依赖）

### 2.3 Controller 位置
*   所有 Controller **必须**位于 `*-service` 模块（如 `blog-system-service`）
*   **严禁**在 `blog-application` 中创建 Controller

### 2.4 微服务就绪原则
*   **严禁跨模块 JOIN**: 禁止编写跨业务模块的 SQL JOIN（如 `sys_user` JOIN `art_article`）
*   **接口化调用**: 跨模块交互必须通过 `*-api` 中定义的接口（如 `RemoteUserService`）

---

## 3. 编码标准

### 3.1 实体类（Entity）
*   使用 MyBatis-Plus 注解: `@TableName`, `@TableId`, `@TableField`
*   **严禁**在 API 响应中直接暴露实体类，必须转换为 DTO

### 3.2 数据传输对象（DTO）
*   必须实现 `Serializable` 接口
*   必须位于 `*-api` 模块
*   使用 `@Schema` 注解提供 API 文档
*   必须实现 `Identifiable<T>` 接口（用于 BaseServiceImpl）

### 3.3 依赖注入
*   **优先使用构造器注入** + Lombok 的 `@RequiredArgsConstructor`
*   **避免**在字段上使用 `@Autowired`

**示例**:
```java
@Service
@RequiredArgsConstructor
public class UserServiceImpl {
    private final UserMapper userMapper;  // ✅ 构造器注入
    private final RoleMapper roleMapper;
}
```

### 3.4 工具类使用
*   优先使用 `StringUtils`（Lang3）、`CollectionUtils`（Collections4）
*   使用 `Preconditions`（Guava）进行参数校验
*   避免手动 null 检查

---

## 4. 测试标准

### 4.1 测试框架
*   **JUnit 5**: 测试框架
*   **Mockito**: Mock 框架
*   **AssertJ**: 断言库

### 4.2 单元测试
*   重点测试 **Service 层**
*   使用 `@ExtendWith(MockitoExtension.class)`
*   **Mock** 所有依赖（Mappers、其他 Services）

### 4.3 集成测试
*   重点测试 **Controller 层**
*   使用 `@SpringBootTest` + `MockMvc`

### 4.4 命名规范
*   测试类: `{TargetClass}Test`
*   测试方法: `should_expectedBehavior_when_state()`

**示例**:
```java
@Test
void should_returnUserDTO_when_validIdProvided() {
    // 测试逻辑
}
```

### 4.5 覆盖率目标
*   Service 层: ≥ 80% 行覆盖率
*   Controller 层: ≥ 70% 行覆盖率
*   关键路径（认证、支付等）: 100% 覆盖率

---

## 5. 开发工作流

1.  **定义 API**: 在 `*-api` 模块中创建 DTOs 和接口
2.  **实现 Service**: 在 `*-service` 模块中创建 Entity、Mapper 和 Service 实现
3.  **实现 Web**: 在 `*-service` 模块中创建 Controller
4.  **编写测试**: 为 Service 编写单元测试，为 Controller 编写集成测试

---

## 6. 关键实现细节

### 6.1 统一 API 响应
*   **规则**: 所有 Controller 方法**必须**返回 `com.blog.common.model.Result<T>`

**示例**:
```java
public Result<UserDTO> getUser() {
    return Result.success(userDTO);
}
```

### 6.2 数据库变更（Flyway）
*   **规则**: **严禁**手动修改数据库 Schema
*   **操作**: 创建版本化 SQL 脚本到 `blog-application/src/main/resources/db`
*   **命名**: `V{version}__{description}.sql`（如 `V1.0.2__add_user_role_indexes.sql`）

### 6.3 异常处理
*   **规则**: Controller 中**禁止**使用 `try-catch` 处理业务逻辑
*   **操作**: 抛出 `com.blog.common.exception.BusinessException` + `ErrorCode`

**示例**:
```java
if (user == null) {
    throw new BusinessException(SystemErrorCode.USER_NOT_FOUND);
}
```

### 6.4 MapStruct 配置
*   **规则**: 转换器接口**必须**使用以下配置:

```java
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
```

### 6.5 安全上下文
*   **规则**: **禁止**将 `userId` 作为 Controller 参数（安全风险）
*   **操作**: 从 Security Context 中获取当前用户（通过 `SecurityUtils` 工具类）

---

## 7. Base Framework 使用（blog-common/base）

### 7.1 Service 层结构
*   所有 Service 实现**必须**继承 `BaseServiceImpl<M, E, V, D, C>`
*   转换器**必须**实现 `BaseConverter<D, E, V>` 并配置正确的 MapStruct 注解
*   所有 DTO **必须**实现 `Identifiable<T>` 接口

### 7.2 自动验证
*   `saveByDto` 和 `updateByDto` 自动使用 JSR-303 注解验证 DTO
*   在 DTO 字段上使用 `@NotNull`, `@Size`, `@Email` 等注解
*   验证失败会抛出 `BusinessException(SystemErrorCode.PARAM_ERROR, ...)`

### 7.3 更新操作
*   **单条更新**: `updateByDto` 是**安全的** - 先加载原实体，再增量合并更新
*   **批量更新**: `updateBatchByDto` 对部分 DTO **不安全** - 直接转换 DTO 到 Entity，不加载原数据

### 7.4 删除操作
*   `removeById` 是**幂等的** - 即使资源不存在也返回 `true`（符合 RESTful 标准）
*   如需验证删除，先用 `getById` 检查存在性

### 7.5 流式查询
*   `streamVo` 需要在 Mapper 和 Service 中自定义实现
*   **Mapper**: 使用 `@Options(resultSetType = ResultSetType.FORWARD_ONLY, fetchSize = 1000)`
*   **Service**: 重写 `streamVo`，使用 `@Transactional(readOnly = true)` 和 `try-with-resources`

---

## 8. Redis 缓存规范

### 8.1 Spring Cache 注解
*   使用 `@Cacheable` 缓存读操作
*   使用 `@CacheEvict` 在数据修改时失效相关缓存
*   使用 `@CachePut` 进行选择性缓存更新（较少使用）

### 8.2 缓存命名规范
*   **格式**: `{模块}:{实体}:{操作}`
*   **示例**: `user:roles`, `article:detail`, `comment:list`
*   **Key 表达式**: 使用 SpEL - `key = "#userId"`, `key = "#articleId"`

### 8.3 默认配置
*   **过期时间**: 30 分钟（在 `RedisConfig` 中配置）
*   **Null 值**: 禁止缓存（防止缓存穿透）
*   **序列化**: Jackson JSON，保留类型信息

### 8.4 缓存失效
*   数据修改时**必须**失效相关缓存

**示例**:
```java
@CacheEvict(value = "user:roles", key = "#userId")
public boolean assignRoleToUser(Long userId, Long roleId) {
    // 实现逻辑
}
```

### 8.5 RedisUtils 使用
*   对于 Spring Cache 未覆盖的复杂操作，使用 `RedisUtils`
*   支持操作: String (SET/GET), Hash (HSET/HGET), Set (SADD), List (LPUSH/RPOP)
*   **必须**设置过期时间，防止内存泄漏

---

## 9. Security 配置

### 9.1 三链架构
*   **链 1 (@Order(1))**: 白名单放行（`/actuator/**`, `/swagger-ui/**`, `/v3/api-docs/**`）
*   **链 2 (@Order(2))**: JWT 认证（`/auth/**`, `/api/**`）
    *   公开端点: `/auth/register`, `/auth/login`
    *   保护端点: 其他所有 `/api/**`
*   **链 3 (@Order(3))**: 默认 HTTP Basic + Form Login（兜底）

### 9.2 JWT Token 标准
*   **算法**: HS256 (HMAC with SHA-256)
*   **密钥**: 至少 256 位（在 `application.yaml` 中配置）
*   **过期时间**: 2 小时（7200000 ms）
*   **Claims**: `username`, `userId`, `roles`（List<String>）

### 9.3 密码编码
*   **算法**: BCrypt，默认成本因子（10）
*   **Bean**: 在 `SecurityConfig.passwordEncoder()` 中配置
*   **严禁**以任何形式记录或暴露密码

### 9.4 方法级安全
*   使用 `@PreAuthorize("hasRole('ADMIN')")` 限制仅管理员访问
*   使用 `@PreAuthorize("hasAnyRole('ADMIN', 'USER')")` 多角色访问
*   优先使用方法级安全而非 URL 级安全，实现细粒度控制

---

## 10. 日志规范

### 10.1 日志级别
*   **ERROR**: 系统错误、意外异常、关键故障
*   **WARN**: 业务异常、验证失败、过时 API 使用
*   **INFO**: 重要业务事件（登录、注册、状态变更）
*   **DEBUG**: 详细执行流程（缓存命中、SQL 查询、方法进出）

### 10.2 日志格式
*   使用 `@Slf4j` 注解（Lombok）
*   包含上下文: `userId`, `operation`, `result`

**示例**:
```java
log.info("用户登录成功: userId={}, username={}", userId, username);
log.warn("用户登录失败: username={}, reason={}", username, reason);
log.error("系统异常: operation={}, error={}", operation, e.getMessage(), e);
```

### 10.3 安全性
*   **严禁**记录密码、Token、API 密钥
*   对 PII 进行脱敏: `email` → `e***@example.com`
*   使用占位符 `{}` 而非字符串拼接（性能考虑）

### 10.4 异常日志
*   意外错误记录完整堆栈: `log.error("...", exception)`
*   业务异常仅记录消息: `log.warn("...")`

---

## 11. RESTful API 设计规范

### 11.1 URL 命名
*   使用 kebab-case: `/api/user-profiles`（不是 `/api/userProfiles`）
*   使用复数名词: `/api/users`（不是 `/api/user`）
*   避免动词: `/api/users`（不是 `/api/getUsers`）
*   版本化（可选）: `/api/v1/users`（为未来预留）

### 11.2 HTTP 方法
*   **GET**: 检索资源 - 必须幂等且安全
*   **POST**: 创建新资源 - 非幂等
*   **PUT**: 完全更新（替换整个资源）- 幂等
*   **PATCH**: 部分更新（修改特定字段）- 幂等
*   **DELETE**: 删除资源 - 幂等

### 11.3 HTTP 状态码
*   **200 OK**: 成功的 GET、PUT、PATCH
*   **201 Created**: 成功的 POST（包含 Location 头）
*   **204 No Content**: 成功的 DELETE
*   **400 Bad Request**: 验证错误、请求格式错误
*   **401 Unauthorized**: 缺少或无效的身份认证
*   **403 Forbidden**: 权限不足
*   **404 Not Found**: 资源不存在
*   **500 Internal Server Error**: 意外的服务器错误

### 11.4 响应格式
*   所有响应使用 `Result<T>` 包装器
*   分页使用 MyBatis-Plus 的 `IPage<T>`
*   错误响应包含 `code`, `message` 和可选的 `data`（错误详情）

---

## 12. 数据库设计规范

### 12.1 表命名
*   小写加下划线: `sys_user`, `art_article`
*   模块前缀: `sys_*`（系统）、`art_*`（文章）、`cmt_*`（评论）、`file_*`（文件）
*   使用单数名词: `sys_user`（不是 `sys_users`）

### 12.2 列命名
*   小写加下划线: `create_time`, `user_id`
*   布尔值前缀: `is_deleted`, `is_active`, `is_enabled`
*   外键: `{引用表名}_id`（如 `user_id`, `role_id`）

### 12.3 主键
*   **类型**: `BIGINT`（用于雪花算法 ID）
*   **策略**: `@TableId(type = IdType.ASSIGN_ID)`（MyBatis-Plus）
*   **严禁**在分布式系统中使用自增 ID

### 12.4 必需审计字段
所有表**必须**包含以下字段:
*   `create_by` BIGINT - 创建者用户 ID
*   `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP - 创建时间
*   `update_by` BIGINT - 最后修改者用户 ID
*   `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP - 更新时间
*   `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 - 逻辑删除标志
*   `version` INT NOT NULL DEFAULT 0 - 乐观锁版本号
*   `remark` VARCHAR(500) - 备注（可选）

### 12.5 索引规范
*   在高频查询列上创建索引
*   在自然唯一键上创建唯一索引（username、email、role_key）
*   使用 Flyway 迁移添加索引 - **严禁**手动添加
*   命名规范: `idx_{列名}` 或 `uk_{列名}`（唯一索引）

---

## 📚 参考文档

*   [项目知识库](knowledge-base.md) - 完整项目文档
*   [架构设计文档](ARCHITECTURE_DESIGN.md) - 系统架构详解
*   [测试指南](TESTING_GUIDE.md) - 全面测试策略
*   [Base Framework 指南](BASE_FRAMEWORK_GUIDE.md) - BaseServiceImpl 使用

---

**版本历史**:
- v2.0 (2025-12-07): 新增 Redis、Security、日志、API、数据库规范
- v1.0 (2025-09-20): 初始版本

**维护人**: liusxml  
**审核**: AI Assistant

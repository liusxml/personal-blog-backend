---
sidebar_position: 1
---

# 开发规范

本文档定义了 Personal Blog Backend 项目的**强制性**规则、编码标准和架构约束。所有开发者（包括 AI 助手）都必须严格遵守。

:::warning 规范的重要性
这些规范不仅是建议，而是**硬性要求**。项目使用 ArchUnit 进行架构测试，违反规范的代码将无法通过 CI/CD 流程。
:::

## 🛠️ 技术栈（不可变更）

### 核心技术

| 技术 | 版本 | 用途 |
|------|------|------|
| **Java** | 21 | 开发语言 |
| **Spring Boot** | 3.5.7 | 应用框架 |
| **MyBatis-Plus** | 3.5.14 | ORM 框架 |
| **MySQL** | 9.4.0 | 关系数据库 |

### 开发工具

- **Lombok** - 消除样板代码（`@Data`, `@RequiredArgsConstructor`, `@Slf4j`）
- **MapStruct** - 高性能 Bean 映射（DTO ↔ Entity）
- **Commons-Lang3 / Collections4 / Guava** - 工具函数库
- **SpringDoc** - OpenAPI/Swagger 文档生成

### 基础设施

- **Redis** - 分布式缓存和会话管理
- **Spring Security** - 认证和授权
- **Flyway** - 数据库版本控制
- **Spring Boot Actuator** - 健康监控

## 🏗️ 架构硬性规则（模块化单体）

### 模块结构

```
blog-modules/
├── blog-*-api/          # API 定义层
│   ├── dto/             # ✅ 仅包含 DTOs
│   ├── enums/           # ✅ 仅包含枚举
│   └── service/         # ✅ 仅包含接口定义
│
└── blog-*-service/      # 服务实现层
    ├── controller/      # ✅ Web 层
    ├── service/         # ✅ 业务逻辑
    ├── entity/          # ✅ 数据库实体
    ├── mapper/          # ✅ 持久层
    └── config/          # ✅ 模块配置
```

:::danger 绝对禁止
- ❌ **`*-api` 模块不能包含**：Entity、Service 实现、任何业务逻辑
- ❌ **`blog-application` 不能包含**：Controller、Service、Entity
- ❌ **跨模块直接依赖**：`blog-article-service` → `blog-system-service`
:::

### 依赖规则

**✅ 允许的依赖**：
```
blog-application  →  blog-*-service
blog-*-service    →  blog-*-api (自己的或其他模块的)
blog-*-service    →  blog-common
blog-*-api        →  blog-common
```

**❌ 严禁的依赖**：
```
blog-*-service  ✖→  blog-*-service  # Service 不能直接依赖 Service
blog-*-api      ✖→  blog-*-service  # API 不能依赖实现
blog-common     ✖→  任何业务模块      # Common 必须保持独立
```

### Controller 位置

:::important 关键规则
所有 Controller **必须**位于 `*-service` 模块（如 `blog-system-service`），**严禁**在 `blog-application` 中创建 Controller。
:::

### 微服务就绪原则

**严禁跨模块数据库 JOIN**：

❌ **错误示例**：
```sql
SELECT a.*, u.username 
FROM art_article a 
JOIN sys_user u ON a.author_id = u.id  -- 跨模块 JOIN
```

✅ **正确做法**：
```java
// 1. 查询文章
List<Article> articles = articleMapper.selectList(...);

// 2. 通过接口获取用户信息
List<Long> authorIds = articles.stream()
    .map(Article::getAuthorId)
    .collect(Collectors.toList());
List<UserDTO> users = remoteUserService.getUsersByIds(authorIds);

// 3. 在内存中组装数据
```

## 💻 编码标准

### Entity（数据库实体）

```java
@Data
@TableName("sys_user")
public class User extends BaseEntity {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    @TableField("username")
    private String username;
    
    @TableField("password_hash")
    private String passwordHash;
}
```

**规范**：
- ✅ 使用 MyBatis-Plus 注解：`@TableName`, `@TableId`, `@TableField`
- ✅ 主键策略：`IdType.ASSIGN_ID`（雪花算法）
- ❌ **严禁**在 API 响应中直接暴露 Entity，必须转换为 DTO

### DTO（数据传输对象）

```java
@Data
@Schema(description = "用户信息")
public class UserDTO implements Serializable, Identifiable<Long> {
    @Schema(description = "用户ID")
    private Long id;
    
    @Schema(description = "用户名")
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度必须在3-20之间")
    private String username;
    
    @Schema(description = "邮箱")
    @Email(message = "邮箱格式不正确")
    private String email;
}
```

**规范**：
- ✅ 必须实现 `Serializable` 接口
- ✅ 必须实现 `Identifiable<T>` 接口（用于 BaseServiceImpl）
- ✅ 必须位于 `*-api` 模块
- ✅ 使用 `@Schema` 注解提供 API 文档
- ✅ 使用 JSR-303 注解进行验证（`@NotNull`, `@Size`, `@Email`）

### 依赖注入

**优先使用构造器注入** + Lombok 的 `@RequiredArgsConstructor`：

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl extends BaseServiceImpl<...> {
    // ✅ final 字段 + 构造器注入
    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserConverter userConverter;
    
    // ❌ 避免字段注入
    // @Autowired
    // private UserMapper userMapper;
}
```

### 工具类使用

**优先使用成熟的工具库**，避免重复造轮子：

```java
// ✅ 使用 StringUtils（Apache Commons Lang3）
if (StringUtils.isBlank(username)) {
    throw new BusinessException(ErrorCode.INVALID_USERNAME);
}

// ✅ 使用 CollectionUtils（Apache Commons Collections4）
if (CollectionUtils.isEmpty(userIds)) {
    return Collections.emptyList();
}

// ✅ 使用 Preconditions（Guava）
Preconditions.checkNotNull(userId, "用户ID不能为null");
```

## 🧪 测试标准

### 测试框架

- **JUnit 5** - 测试框架
- **Mockito** - Mock 框架
- **AssertJ** - 流式断言库

### 单元测试（Service 层）

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("用户服务测试")
class UserServiceImplTest {
    
    @Mock
    private UserMapper userMapper;
    
    @Mock
    private UserConverter userConverter;
    
    @InjectMocks
    private UserServiceImpl userService;
    
    @Test
    @DisplayName("应该成功注册用户")
    void should_register_user_successfully() {
        // Given
        UserDTO userDTO = new UserDTO();
        userDTO.setUsername("testuser");
        userDTO.setPassword("password123");
        
        given(userMapper.selectByUsername("testuser")).willReturn(null);
        
        // When
        userService.register(userDTO);
        
        // Then
        verify(userMapper).insert(any(User.class));
    }
}
```

**规范**：
- ✅ 使用 `@ExtendWith(MockitoExtension.class)`
- ✅ **Mock** 所有依赖（Mappers、其他 Services）
- ✅ 命名：`{TargetClass}Test`
- ✅ 方法命名：`should_expectedBehavior_when_state()`

### 集成测试（Controller 层）

```java
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("用户控制器集成测试")
class UserControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    @DisplayName("应该返回200当登录成功")
    void should_return_200_when_login_success() throws Exception {
        mockMvc.perform(post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").exists());
    }
}
```

### 覆盖率目标

- **Service 层**: ≥ 80% 行覆盖率
- **Controller 层**: ≥ 70% 行覆盖率
- **关键路径**（认证、支付等）: 100% 覆盖率

## 🔑 关键实现细节

### 统一 API 响应

**规则**：所有 Controller 方法**必须**返回 `com.blog.common.model.Result<T>`

```java
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    
    @GetMapping("/{id}")
    public Result<UserDTO> getUser(@PathVariable Long id) {
        UserDTO user = userService.getById(id);
        return Result.success(user);
    }
    
    @PostMapping
    public Result<UserDTO> createUser(@Valid @RequestBody UserDTO userDTO) {
        UserDTO created = userService.saveByDto(userDTO);
        return Result.success(created);
    }
}
```

### 数据库变更（Flyway）

**规则**：**严禁**手动修改数据库 Schema

**操作流程**：
1. 创建版本化 SQL 脚本到 `blog-application/src/main/resources/db/migration/`
2. 命名格式：`V{version}__{description}.sql`
3. 示例：`V1.0.2__add_user_email_index.sql`

```sql
-- V1.0.2__add_user_email_index.sql
CREATE UNIQUE INDEX uk_email ON sys_user(email);

ALTER TABLE sys_user 
ADD COLUMN phone VARCHAR(20) COMMENT '手机号';
```

### 异常处理

**规则**：Controller 中**禁止**使用 `try-catch` 处理业务逻辑

**操作**：抛出 `BusinessException` + `ErrorCode`

```java
@Service
public class UserServiceImpl extends BaseServiceImpl<...> {
    
    public UserDTO getUserById(Long userId) {
        User user = userMapper.selectById(userId);
        
        // ✅ 抛出业务异常，由全局异常处理器统一处理
        if (user == null) {
            throw new BusinessException(SystemErrorCode.USER_NOT_FOUND);
        }
        
        return userConverter.toDto(user);
    }
}
```

### MapStruct 配置

**规则**：转换器接口**必须**使用以下配置：

```java
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface UserConverter extends BaseConverter<UserDTO, User, UserVO> {
    // 自定义映射（如需要）
    @Mapping(source = "passwordHash", target = "password", ignore = true)
    UserDTO toDto(User entity);
}
```

### 安全上下文

**规则**：**禁止**将 `userId` 作为 Controller 参数（安全风险）

❌ **错误做法**：
```java
@PostMapping("/articles")
public Result<ArticleDTO> createArticle(
    @RequestParam Long userId,  // ❌ 用户可以随意伪造
    @RequestBody ArticleDTO articleDTO) {
    // ...
}
```

✅ **正确做法**：
```java
@PostMapping("/articles")
public Result<ArticleDTO> createArticle(@RequestBody ArticleDTO articleDTO) {
    // ✅ 从安全上下文中获取当前登录用户
    Long currentUserId = SecurityUtils.getCurrentUserId();
    articleDTO.setAuthorId(currentUserId);
    // ...
}
```

## 📚 Base Framework 使用

### Service 层结构

所有 Service 实现**必须**继承 `BaseServiceImpl<M, E, V, D, C>`：

```java
@Service
@RequiredArgsConstructor
public class UserServiceImpl 
        extends BaseServiceImpl<UserMapper, User, UserVO, UserDTO, UserConverter>
        implements UserService, RemoteUserService {
    
    // BaseServiceImpl 已提供以下方法，无需重复实现：
    // - saveByDto(DTO dto)
    // - updateByDto(DTO dto)
    // - removeById(ID id)
    // - getById(ID id)
    // - listVo(Wrapper queryWrapper)
    // - pageVo(IPage page, Wrapper queryWrapper)
}
```

### 自动验证

`saveByDto` 和 `updateByDto` 自动验证 DTOs：

```java
@Data
public class UserDTO implements Identifiable<Long> {
    private Long id;
    
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20)
    private String username;
    
    @Email(message = "邮箱格式不正确")
    private String email;
}

// 调用时自动验证
userService.saveByDto(userDTO);  // 验证失败会抛出 BusinessException
```

### 更新操作注意事项

- **单条更新**：`updateByDto` 是**安全的** - 先加载原实体，再增量合并
- **批量更新**：`updateBatchByDto` 对部分 DTO **不安全** - 直接转换，不加载原数据

## 📖 延伸阅读

<!-- 以下页面即将推出 -->
- **Base Framework 指南** - BaseServiceImpl 详细使用
- **工作流程** - 推荐的开发流程
- **MapStruct 最佳实践** - 高效对象映射

---

**备注**：完整的开发规范请参考项目根目录的 [development-standards.md](https://github.com/liusxml/personal-blog-backend/blob/main/docs/development-standards.md)。

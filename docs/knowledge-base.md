# 🎓 Personal Blog Backend - 项目知识库

> **版本**: 1.0-SNAPSHOT  
> **Java**: 21  
> **Spring Boot**: 3.5.7  
> **作者**: liusxml

---

## 📐 架构设计

### 整体架构
**模块化单体架构 (Modular Monolith)**，为微服务演进预留扩展性。

```
personal-blog-backend/
├── blog-common/              # 🔧 公共基础设施
├── blog-application/         # 🚀 应用启动入口
├── blog-admin-server/        # 📊 监控管理服务器
└── blog-modules/             # 📦 业务模块集合
    ├── blog-module-system/   # 用户、角色、权限
    ├── blog-module-article/  # 文章管理
    ├── blog-module-comment/  # 评论系统
    └── blog-module-file/     # 文件服务
```

### 模块分层策略

每个业务模块采用 **API-Service 双层分离**：

```
blog-module-{name}/
├── blog-{name}-api/          # 接口定义层
│   ├── dto/                  # 数据传输对象
│   ├── vo/                   # 视图对象
│   └── RemoteService.java   # 跨模块接口
└── blog-{name}-service/      # 服务实现层
    ├── entity/               # JPA实体
    ├── mapper/               # MyBatis Mapper
    ├── service/              # 业务逻辑
    ├── controller/           # REST控制器
    └── converter/            # DTO-Entity转换
```

**依赖流向**: `Application → Service → API → Common`

---

## 🛠️ 技术栈详解

### 核心框架
| 技术 | 版本 | 用途 |
|------|------|------|
| **Spring Boot** | 3.5.7 | 应用框架 |
| **MyBatis-Plus** | 3.5.14 | ORM + CRUD增强 |
| **MySQL Connector** | 9.4.0 | 数据库驱动 |
| **Flyway** | 自动管理 | 数据库版本控制 |

### 安全认证
| 技术 | 版本 | 用途 |
|------|------|------|
| **Spring Security** | 6.x | 安全框架 |
| **JJWT** | 0.13.0 | JWT Token |
| **BCrypt** | - | 密码加密 |

### 缓存与性能
| 技术 | 版本 | 用途 |
|------|------|------|
| **Spring Cache** | - | 缓存抽象 |
| **Redis** | - | 分布式缓存 |
| **Caffeine** | 3.2.2 | 本地缓存 |

### 开发工具
| 技术 | 版本 | 用途 |
|------|------|------|
| **Lombok** | 1.18.42 | 消除样板代码 |
| **MapStruct** | 1.6.3 | Bean映射 |
| **SpringDoc** | 2.8.14 | API文档 |

### 监控运维
| 技术 | 版本 | 用途 |
|------|------|------|
| **Actuator** | - | 健康监控 |
| **Spring Boot Admin** | 3.5.5 | 可视化管理 |
| **Micrometer** | 1.16.0 | 指标采集 |

---

## 📦 blog-common 模块详解

### 核心组件

#### 1. BaseServiceImpl (通用Service基类)
**路径**: `com.blog.common.base.BaseServiceImpl`

**功能**:
```java
public abstract class BaseServiceImpl<M, E, V, D, C> {
    // CRUD操作
    Optional<V> getVoById(Serializable id);
    List<V> listVo(Wrapper<E> wrapper);
    IPage<V> pageVo(Page<E> page, Wrapper<E> wrapper);
    
    // 安全更新（增量更新，不覆盖已有数据）
    boolean updateByDto(D dto);  // ✅ 加载原实体 → 增量更新
    
    // 批量操作
    List<Serializable> batchSaveByDto(List<D> dtoList);
    boolean updateBatchByDto(List<D> dtoList);  // ⚠️ 直接转换DTO
    
    // 自动校验（JSR-303）
    protected void validate(D dto);
}
```

**设计亮点**:
- ✅ 自动DTO校验（JSR-303）
- ✅ 安全更新：`updateByDto` 先加载原实体，再增量合并
- ✅ 泛型设计：支持任意 Mapper/Entity/DTO/VO/Converter
- ✅ 钩子方法：`preSave()`, `preUpdate()`

#### 2. BaseConverter (MapStruct转换器接口)
**路径**: `com.blog.common.base.BaseConverter`

```java
public interface BaseConverter<D, E, V> {
    E dtoToEntity(D dto);
    V entityToVo(E entity);
    List<E> dtoListToEntityList(List<D> dtoList);
    List<V> entityListToVoList(List<E> entityList);
    
    // ⭐ 核心方法：增量更新
    void updateEntityFromDto(D dto, @MappingTarget E entity);
}
```

**实现规范**:
```java
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface UserConverter extends BaseConverter<UserDTO, SysUser, UserVO> {
    // MapStruct自动生成实现
}
```

#### 3. Result (统一响应结果)
**路径**: `com.blog.common.model.Result`

```java
public record Result<T>(int code, String message, @Nullable T data) {
    // 成功
    Result.success();
    Result.success(data);
    
    // 失败
    Result.error(ErrorCode errorCode);
    Result.error(ErrorCode errorCode, String message);
}
```

#### 4. 异常体系
**路径**: `com.blog.common.exception.*`

```
RuntimeException
└── BusinessException (业务异常基类)
    ├── EntityNotFoundException (404)
    └── OperationFailedException (500)
```

**使用示例**:
```java
if (user == null) {
    throw new BusinessException(SystemErrorCode.USER_NOT_FOUND);
}
```

#### 5. JwtTokenProvider (JWT工具)
**路径**: `com.blog.common.security.JwtTokenProvider`

```java
@Component
public class JwtTokenProvider {
    String generateToken(UserDetails userDetails, Long userId);
    boolean validateToken(String token);
    String getUsernameFromToken(String token);
    Long getUserIdFromToken(String token);
    List<String> getRolesFromToken(String token);
}
```

#### 6. RedisUtils (Redis工具类)
**路径**: `com.blog.common.utils.RedisUtils`

**功能**:
- String操作：SET, GET, INCR, DECR, SETNX
- Hash操作：HSET, HGET, HDEL
- Set操作：SADD, SMEMBERS
- List操作：LPUSH, RPOP, LRANGE
- 通用操作：DELETE, EXPIRE, HASKEY

**特性**:
- ✅ 参数校验 (Guava Preconditions)
- ✅ 日志记录 (SLF4J)
- ✅ 类型安全 (Optional支持)

---

## 🚀 blog-application 模块详解

### 核心配置类

#### 1. SecurityConfig (安全配置)
**路径**: `com.blog.config.SecurityConfig`

**三链架构** (多 `SecurityFilterChain`):

```java
@Bean @Order(1)
SecurityFilterChain permitAllChain(HttpSecurity http) {
    // 白名单：/actuator/health, /swagger-ui/**
}

@Bean @Order(2)
SecurityFilterChain jwtChain(HttpSecurity http) {
    // JWT认证：/auth/**, /api/**
    // 公开：/auth/register, /auth/login
}

@Bean @Order(3)
SecurityFilterChain defaultChain(HttpSecurity http) {
    // 兜底：HTTP Basic + Form Login
}
```

**设计优势**:
- ✅ 白名单优先，避免误拦截监控端点
- ✅ JWT无状态认证，适合RESTful API
- ✅ 多链隔离，易于微服务拆分

#### 2. RedisConfig (Redis配置)
**路径**: `com.blog.config.RedisConfig`

**V2.0 改进**:
```java
@EnableCaching  // ⭐ 新增：支持 @Cacheable
@Configuration
public class RedisConfig {
    
    @Bean
    RedisTemplate<String, Object> redisTemplate(...) {
        // ✅ 自定义ObjectMapper
        // ✅ 支持Java 8时间类型
        // ✅ 保留类型信息
    }
    
    @Bean
    RedisCacheManager cacheManager(...) {
        // ✅ 默认30分钟过期
        // ✅ 禁止缓存null值
    }
}
```

#### 3. MybatisPlusConfig
**路径**: `com.blog.config.MybatisPlusConfig`

**插件配置**:
```java
MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());  // 防全表更新
interceptor.addInnerInterceptor(new PaginationInnerInterceptor());   // 分页
interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor()); // 乐观锁
```

#### 4. GlobalExceptionHandler (全局异常处理)
**路径**: `com.blog.handler.GlobalExceptionHandler`

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)  // 业务异常返回200
    Result<?> handleBusinessException(...);
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)  // 参数校验失败返回400
    Result<?> handleValidationException(...);
    
    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)  // 实体未找到返回404
    Result<?> handleNotFoundException(...);
    
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)  // 未知异常返回500
    Result<?> handleUnexpectedException(...);
}
```

### 配置文件 (application.yaml)

```yaml
server:
  port: 8080
  shutdown: graceful

spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/blog_db
    username: root
    password: ***
    hikari:
      maximum-pool-size: 20
      minimum-idle: 20

mybatis-plus:
  global-config:
    db-config:
      id-type: assign_uuid
      logic-delete-field: isDeleted

app:
  security:
    permit-all-urls:
      - /v3/api-docs/**
      - /swagger-ui/**
      - /actuator/**
    jwt-secret: ***
    jwt-expiration: 7200000  # 2小时

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,env,prometheus
```

### 数据库迁移 (Flyway)

**脚本位置**: `src/main/resources/db/`

| 脚本 | 描述 |
|------|------|
| `V1.0.0__init_schema.sql` | 初始化表结构 (sys_user, sys_role, sys_user_role) |
| `V1.0.1__init_system_data.sql` | 初始化系统数据 (默认角色) |
| `V1.0.2__add_user_role_indexes.sql` | 添加性能索引 (username, email, role_key) |

---

## 🛡️ blog-module-system 详解

### 模块职责
- 用户注册/登录
- JWT Token生成
- RBAC权限管理
- 用户信息管理

### 核心实体

#### SysUser (用户实体)
```java
@TableName("sys_user")
public class SysUser {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    private String username;  // ⭐ UNIQUE索引
    private String nickname;
    private String password;  // BCrypt加密
    private String email;     // ⭐ UNIQUE索引
    private String avatar;
    private Integer status;   // ⭐ 普通索引
    
    @Version
    private Integer version;  // 乐观锁
    
    @TableLogic
    private Integer isDeleted;  // 逻辑删除
}
```

**建议索引** (已在 V1.0.2 中创建):
- `idx_username` (UNIQUE)
- `idx_email` (UNIQUE)
- `idx_status`
- `idx_create_time`

#### SysRole (角色实体)
```java
@TableName("sys_role")
public class SysRole {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    private String roleName;  // 角色名称
    private String roleKey;   // ⭐ UNIQUE索引 (权限标识)
    private Integer status;
}
```

**建议索引**:
- `idx_role_key` (UNIQUE)
- `idx_role_status`

#### sys_user_role (关联表)
```sql
CREATE TABLE sys_user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id)
);
```

**架构说明**: 
- ✅ 属于 `blog-module-system` 内部
- ✅ 模块内JOIN是允许的
- ❌ 跨模块JOIN是禁止的

### 核心服务

#### UserServiceImpl
```java
@Service
public class UserServiceImpl extends BaseServiceImpl<...> implements IUserService {
    
    // 用户注册
    UserVO register(RegisterDTO dto) {
        // 1. 检查用户名/邮箱唯一性
        // 2. BCrypt加密密码
        // 3. 分配默认角色 (RoleConstants.DEFAULT_USER_ROLE)
        // 4. 保存用户
    }
    
    // 用户登录（带缓存）
    LoginVO login(LoginDTO dto) {
        // 1. 查询用户（支持username或email）
        // 2. 验证密码
        // 3. 查询角色（⭐ 使用缓存）
        // 4. 生成JWT Token
    }
    
    // ⭐ 角色缓存
    @Cacheable(value = "user:roles", key = "#userId")
    List<String> getUserRoleKeys(Long userId) {
        // 缓存键：user:roles:{userId}
        // 过期时间：30分钟
    }
    
    // ⭐ 缓存失效
    @CacheEvict(value = "user:roles", key = "#userId")
    void evictUserRolesCache(Long userId);
}
```

#### RoleServiceImpl
```java
@Service
public class RoleServiceImpl extends BaseServiceImpl<...> {
    
    @CacheEvict(value = "user:roles", key = "#userId")
    boolean assignRoleToUser(Long userId, Long roleId);
    
    @CacheEvict(value = "user:roles", key = "#userId")
    boolean removeRoleFromUser(Long userId, Long roleId);
}
```

#### RemoteUserServiceImpl (跨模块服务)
```java
@Service
public class RemoteUserServiceImpl implements RemoteUserService {
    
    List<UserDTO> getUsersByIds(List<Long> userIds) {
        // 供其他模块批量查询用户
    }
    
    UserDTO getUserById(Long userId) {
        // 供其他模块查询单个用户
    }
}
```

### 常量定义

#### RoleConstants
```java
public class RoleConstants {
    public static final String DEFAULT_USER_ROLE = "USER";
    public static final String ADMIN_ROLE = "ADMIN";
    public static final String ROLE_PREFIX = "ROLE_";
}
```

---

## 🎯 最佳实践总结

### 1. 架构规范
- ✅ **模块隔离**: Service模块之间严禁直接依赖
- ✅ **接口调用**: 跨模块通过 `RemoteService` 接口
- ✅ **SQL限制**: 禁止跨模块JOIN

### 2. 代码规范
- ✅ **依赖注入**: 优先使用构造器注入 (`@RequiredArgsConstructor`)
- ✅ **DTO验证**: 使用 JSR-303 注解 (`@NotNull`, `@Size`, `@Email`)
- ✅ **异常处理**: 使用 `BusinessException` + `ErrorCode`
- ✅ **不暴露实体**: Controller只返回VO，不返回Entity

### 3. 安全规范
- ✅ **密码加密**: BCrypt (成本因子默认10)
- ✅ **Token认证**: JWT (有效期2小时)
- ✅ **权限控制**: `@PreAuthorize("hasRole('ADMIN')")`

### 4. 性能优化
- ✅ **缓存策略**: Spring Cache + Redis
- ✅ **数据库索引**: 高频查询字段建索引
- ✅ **连接池**: HikariCP (最大20连接)
- ✅ **乐观锁**: `@Version` 防止并发冲突

### 5. 测试规范
- ✅ **单元测试**: Service层 + Mockito
- ✅ **集成测试**: Controller层 + MockMvc
- ✅ **测试命名**: `should_expectedBehavior_when_state()`

---

## 📚 关键配置速查

### Maven编译器
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <source>21</source>
        <target>21</target>
        <annotationProcessorPaths>
            <path><!-- Lombok --></path>
            <path><!-- MapStruct --></path>
            <path><!-- lombok-mapstruct-binding --></path>
        </annotationProcessorPaths>
    </configuration>
</plugin>
```

### MyBatis-Plus 配置
```yaml
mybatis-plus:
  global-config:
    db-config:
      id-type: assign_uuid           # 雪花算法ID
      logic-delete-field: isDeleted  # 逻辑删除字段
      logic-delete-value: 1
      logic-not-delete-value: 0
  configuration:
    map-underscore-to-camel-case: true
    call-setters-on-nulls: true
    log-impl: org.apache.ibatis.logging.slf4j.Slf4jImpl
```

---

## 🚦 启动流程

### 1. 环境准备
```bash
# 1. 安装 Java 21
java -version  # 验证

# 2. 安装 MySQL 8+
# 创建数据库: blog_db

# 3. 安装 Redis (可选)
redis-server
```

### 2. 配置修改
编辑 `blog-application/src/main/resources/application.yaml`:
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/blog_db
    username: root
    password: your_password
```

### 3. 编译运行
```bash
# 方式一：Maven插件
mvn spring-boot:run -pl blog-application

# 方式二：打包运行
mvn clean package
java -jar blog-application/target/*.jar
```

### 4. 验证
- API文档: http://localhost:8080/swagger-ui.html
- 健康检查: http://localhost:8080/actuator/health
- Admin监控: http://localhost:9000

---

## 📖 API设计示例

### 统一响应格式
```json
{
  "code": 0,
  "message": "success",
  "data": { ... }
}
```

### 错误响应
```json
{
  "code": 4001,
  "message": "用户名已存在",
  "data": null
}
```

### 分页响应
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "records": [...],
    "total": 100,
    "size": 10,
    "current": 1,
    "pages": 10
  }
}
```

---

## 🔍 故障排查

### 常见问题

**Q1: Flyway migration failed**
- 检查数据库连接配置
- 确认 `db/migration` 目录下SQL语法正确
- 删除 `flyway_schema_history` 表重试（仅开发环境）

**Q2: JWT Token验证失败**
- 检查 `app.security.jwt-secret` 长度 (至少256位)
- 确认Token未过期 (2小时有效期)
- 查看日志: `JWT Token 验证失败: ...`

**Q3: Redis连接失败**
- 确认Redis服务已启动: `redis-cli ping`
- 检查连接配置: `spring.data.redis.host/port`

**Q4: MapStruct未生成实现类**
- 执行 `mvn clean compile` 重新生成
- 检查 `target/generated-sources/annotations/`
- 确认 `lombok-mapstruct-binding` 已配置

---

## 📌 下一步建议

### 短期优化
1. 补充集成测试覆盖
2. 添加API限流 (Redis + Guava RateLimiter)
3. 实现操作日志记录

### 中期规划
1. 引入消息队列 (RabbitMQ/Kafka)
2. 实现分布式事务 (Seata)
3. 添加全文检索 (Elasticsearch)

### 长期演进
1. 微服务拆分 (Spring Cloud)
2. 服务网格 (Istio)
3. K8s容器化部署

---

**文档版本**: v1.0  
**最后更新**: 2025-12-07  
**维护人**: liusxml & AI Assistant

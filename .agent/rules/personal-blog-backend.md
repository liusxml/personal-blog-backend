---
trigger: always_on
---

---
trigger: always_on
---

# 🤖 Agent Rules for Personal Blog Backend

This document defines the **mandatory** rules, coding standards, and architectural constraints for any AI Agent working on this project.

## 1. Technology Stack (Immutable)
*   **Language**: Java 21
*   **Framework**: Spring Boot 3.5.7
*   **ORM**: MyBatis-Plus 3.5.14
*   **Database**: MySQL 9.4.0
*   **Cache**: Redis (Lettuce) + Caffeine (Local)
*   **Security**: Spring Security 6 + JWT (JJWT 0.13.0)
*   **Tools**:
    *   **Lombok**: For boilerplate reduction (`@Data`, `@RequiredArgsConstructor`, `@Slf4j`).
    *   **MapStruct**: For efficient Bean mapping (DTO <-> Entity).
    *   **Commons-Lang3 / Collections4 / Guava**: For utility functions.
    *   **SpringDoc**: For OpenAPI/Swagger documentation.

## 2. Project Modules

| Module | Type | Description |
|:---|:---|:---|
| `blog-application` | Startup | Application entry, global configs, handlers |
| `blog-common` | Library | Base framework, exceptions, utilities, security |
| `blog-system-api` | API | User/Role DTOs, VOs, interfaces |
| `blog-system-service` | Service | Auth, User, Role controllers and services |
| `blog-article-api/service` | Business | Article management (placeholder) |
| `blog-comment-api/service` | Business | Comment management (placeholder) |
| `blog-file-api/service` | Business | File storage with Strategy pattern |
| `blog-admin-server` | Monitor | Spring Boot Admin server (port 9000) |

## 3. Architectural Hard Rules (Modular Monolith)
*   **Module Structure**:
    *   `*-api`: DTOs, VOs, Interfaces, Enums ONLY. **NO** Entities, **NO** Logic.
    *   `*-service`: Controllers, Services, Entities, Mappers, Converters.
    *   `blog-application`: Startup class and global config ONLY.
*   **Dependency Rule**:
    *   ✅ `Service` -> `API` -> `Common`
    *   ❌ `Service` -> `Service` (Strictly Forbidden)
*   **Controller Location**:
    *   All Controllers **MUST** reside in the `*-service` module.
    *   **NEVER** place Controllers in `blog-application`.
*   **Microservices Readiness**:
    *   **NO Cross-Module Joins**: Never write SQL that joins tables from different modules.
    *   **Interface-Based Calls**: Cross-module interaction via `*-api` Interfaces.

## 4. Database Design

### 4.1 Table Naming
*   Prefix: `sys_` (system), `art_` (article), `cmt_` (comment), `file_` (file)
*   Example: `sys_user`, `sys_role`, `sys_user_role`

### 4.2 Common Fields (All Tables)
| Field | Type | Description |
|:---|:---|:---|
| `id` | BIGINT | Primary key (Snowflake `ASSIGN_ID`) |
| `version` | INT | Optimistic lock (`@Version`) |
| `create_by` | BIGINT | Creator ID (auto-fill) |
| `create_time` | DATETIME | Creation time (auto-fill) |
| `update_by` | BIGINT | Updater ID (auto-fill) |
| `update_time` | DATETIME | Update time (auto-fill) |
| `is_deleted` | TINYINT | Logical delete (0=active, 1=deleted) |

### 4.3 Schema Changes
*   **Rule**: **NEVER** modify database schema manually.
*   **Action**: Create versioned SQL in `blog-application/src/main/resources/db/`.
*   **Naming**: `V{version}__{description}.sql` (e.g., `V1.0.2__add_article_table.sql`).

## 5. Coding Standards

### 5.1 Entities
*   Use `@TableName`, `@TableId`, `@TableField` from MyBatis-Plus.
*   Use `@TableLogic` for `isDeleted`, `@Version` for optimistic lock.
*   **NEVER** expose Entities in API responses; always map to VOs.

### 5.2 DTOs & VOs
*   DTOs: Request objects, must implement `Serializable` and `Identifiable<T>`.
*   VOs: Response objects, use `@Schema` for API documentation.
*   Location: `*-api` module only.

### 5.3 Dependency Injection
*   **Prefer** `@RequiredArgsConstructor` constructor injection.
*   **Avoid** `@Autowired` on fields.

### 5.4 Utilities
*   Use `StringUtils` (Lang3), `CollectionUtils` (Collections4), not manual null checks.
*   Use `ImmutableList` (Guava) for thread-safe lists.

## 6. API Response & Exception Handling

### 6.1 Unified Response
*   **Rule**: All Controller methods **MUST** return `com.blog.common.model.Result<T>`.
*   **Usage**:
    ```java
    public Result<UserVO> getUser() {
        return Result.success(userVO);
    }
    ```

### 6.2 Exception Handling
*   **Rule**: Do **NOT** use try-catch in Controllers for business logic.
*   **Action**: Throw `BusinessException` with `ErrorCode`.
*   **Usage**:
    ```java
    if (user == null) {
        throw new BusinessException(SystemErrorCode.USER_NOT_FOUND);
    }
    ```

### 6.3 Common Error Codes
| Code | Constant | HTTP Status |
|:---|:---|:---|
| `PARAM_ERROR` | SystemErrorCode | 400 |
| `UNAUTHORIZED` | SystemErrorCode | 401 |
| `ACCESS_DENIED` | SystemErrorCode | 403 |
| `NOT_FOUND` | SystemErrorCode | 404 |
| `SYSTEM_ERROR` | SystemErrorCode | 500 |

## 7. Security

### 7.1 Authentication Flow
*   Login: `POST /api/v1/auth/login` -> returns JWT token
*   Token: Bearer token in `Authorization` header
*   Expiration: 2 hours (configurable via `app.security.jwt-expiration`)

### 7.2 Security Context
*   **Rule**: Do **NOT** pass `userId` as Controller parameter.
*   **Action**: Use `SecurityUtils.getCurrentUserId()` to get current user.

### 7.3 Role Constants
*   Default user role: `RoleConstants.DEFAULT_USER_ROLE` = `"USER"`
*   Role format in token: `"ROLE_USER"`, `"ROLE_ADMIN"`

### 7.4 Public Endpoints (No Auth Required)
*   `/api/v1/auth/register`, `/api/v1/auth/login`
*   `/actuator/**`, `/swagger-ui/**`, `/v3/api-docs/**`

## 8. Caching

### 8.1 Cache Names & Keys
| Cache Name | Key Pattern | TTL | Description |
|:---|:---|:---|:---|
| `user:roles` | `{userId}` | 30min | User role list |

### 8.2 Cache Annotations
*   **Usage**:
    ```java
    @Cacheable(value = "user:roles", key = "#userId")
    public List<String> getUserRoleKeys(Long userId) { ... }

    @CacheEvict(value = "user:roles", key = "#userId")
    public void evictUserRolesCache(Long userId) { ... }
    ```

### 8.3 Cache Keys Constant
*   Use `com.blog.common.constants.CacheKeys` for key prefixes.

## 9. Base Framework (blog-common/base)

### 9.1 Service Layer Structure
*   **Extend**: `BaseServiceImpl<M, E, V, D, C>` for all Service implementations.
*   **Converter**: Implement `BaseConverter<D, E, V>` with:
    ```java
    @Mapper(componentModel = "spring", 
            nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    ```
*   **DTO Constraint**: All DTOs must implement `Identifiable<T>`.

### 9.2 CRUD Operations
| Method | Behavior |
|:---|:---|
| `saveByDto(D)` | Validates DTO, converts to Entity, saves, returns ID |
| `updateByDto(D)` | **Safe**: Loads existing entity, applies changes incrementally |
| `updateBatchByDto(List<D>)` | **Unsafe**: Direct DTO->Entity, no load |
| `removeById(id)` | Idempotent, returns `true` even if not exists |
| `getVoById(id)` | Returns `Optional<V>` |

### 9.3 Lifecycle Hooks
*   Override `preSave(E entity)` for pre-insert logic.
*   Override `preUpdate(E entity)` for pre-update logic.

## 10. Testing Standards

### 10.1 Frameworks
*   JUnit 5, Mockito, AssertJ

### 10.2 Unit Tests (Service Layer)
*   **Usage**:
    ```java
    @ExtendWith(MockitoExtension.class)
    class UserServiceImplTest {
        @Mock private UserMapper userMapper;
        @InjectMocks private UserServiceImpl userService;
    }
    ```

### 10.3 Integration Tests (Controller Layer)
*   **Usage**:
    ```java
    @SpringBootTest
    @AutoConfigureMockMvc
    class AuthControllerTest {
        @Autowired private MockMvc mockMvc;
    }
    ```

### 10.4 Naming Convention
*   Class: `TargetClassTest`
*   Method: `should_expectedBehavior_when_state()`

## 11. Development Workflow

1. **Define API**: Create DTOs/VOs and Interfaces in `*-api`.
2. **Database**: Create Flyway script if new table needed.
3. **Implement Service**: Create Entity, Mapper, Converter, Service in `*-service`.
4. **Implement Controller**: Create Controller in `*-service`, return `Result<T>`.
5. **Test**: Write Unit Test for Service, Integration Test for Controller.
6. **Document**: Add `@Schema` annotations for OpenAPI.

---

## Quick Reference

### Key Classes
| Class | Location | Purpose |
|:---|:---|:---|
| `Result<T>` | blog-common | Unified API response |
| `BusinessException` | blog-common | Business exception |
| `SystemErrorCode` | blog-common | Error code enum |
| `BaseServiceImpl` | blog-common | Generic CRUD base |
| `BaseConverter` | blog-common | MapStruct converter interface |
| `JwtTokenProvider` | blog-common | JWT generation/validation |
| `SecurityUtils` | blog-common | Get current user from context |
| `GlobalExceptionHandler` | blog-application | Exception -> Result mapping |
| `SecurityConfig` | blog-application | Multi-chain security config |

### Key Paths
| Path | Description |
|:---|:---|
| `blog-application/src/main/resources/db/` | Flyway SQL scripts |
| `blog-application/src/main/resources/application.yaml` | Main config |
| `blog-*/src/main/resources/mapper/` | MyBatis XML mappers |

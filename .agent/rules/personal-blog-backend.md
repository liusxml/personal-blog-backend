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
*   **Tools**:
    *   **Lombok**: For boilerplate reduction (`@Data`, `@RequiredArgsConstructor`, `@Slf4j`).
    *   **MapStruct**: For efficient Bean mapping (DTO <-> Entity).
    *   **Commons-Lang3 / Collections4 / Guava**: For utility functions.
    *   **SpringDoc**: For OpenAPI/Swagger documentation.

## 2. Architectural Hard Rules (Modular Monolith)
*   **Module Structure**:
    *   `*-api`: DTOs, Interfaces, Enums ONLY. **NO** Entities, **NO** Logic.
    *   `*-service`: Controllers, Services, Entities, Mappers.
    *   `blog-application`: Startup class and global config ONLY.
*   **Dependency Rule**:
    *   ✅ `Service` -> `API`
    *   ❌ `Service` -> `Service` (Strictly Forbidden)
*   **Controller Location**:
    *   All Controllers **MUST** reside in the `*-service` module (e.g., `blog-system-service`).
    *   **NEVER** place Controllers in `blog-application`.
*   **Microservices Readiness**:
    *   **NO Cross-Module Joins**: Never write SQL that joins tables from different business modules (e.g., `sys_user` join `art_article`).
    *   **Interface-Based Calls**: Cross-module interaction must go through Interfaces defined in `*-api`.

## 3. Coding Standards
*   **Entities**:
    *   Use `@TableName`, `@TableId`, `@TableField` from MyBatis-Plus.
    *   Do **NOT** expose Entities in API responses; always map to DTOs.
*   **DTOs**:
    *   Must implement `Serializable`.
    *   Located in `*-api` module.
    *   Use `@Schema` for API documentation.
*   **Dependency Injection**:
    *   Prefer **Constructor Injection** using Lombok's `@RequiredArgsConstructor`.
    *   Avoid `@Autowired` on fields.
*   **Utilities**:
    *   Prefer `StringUtils` (Lang3), `CollectionUtils` (Collections4) over manual null checks.

## 4. Testing Standards
*   **Frameworks**: JUnit 5, Mockito, AssertJ.
*   **Unit Tests**:
    *   Focus on `Service` layer.
    *   Use `@ExtendWith(MockitoExtension.class)`.
    *   **Mock** all dependencies (Mappers, other Services).
*   **Integration Tests**:
    *   Focus on `Controller` layer.
    *   Use `@SpringBootTest` + `MockMvc`.
*   **Naming**:
    *   Classes: `TargetClassTest`
    *   Methods: `should_expectedBehavior_when_state()`

## 5. Workflow
1.  **Define API**: Create DTOs and Interfaces in `*-api`.
2.  **Implement Service**: Create Entity, Mapper, and Service Impl in `*-service`.
3.  **Implement Web**: Create Controller in `*-service`.
4.  **Test**: Write Unit Test for Service, Integration Test for Controller.

## 6. Critical Implementation Details (Optimized)

### 6.1 Unified API Response
*   **Rule**: All Controller methods **MUST** return `com.blog.common.model.Result<T>`.
*   **Usage**:
    ```java
    public Result<UserDTO> getUser() {
        return Result.success(userDTO);
    }
    ```

### 6.2 Database Changes (Flyway)
*   **Rule**: **NEVER** modify the database schema manually.
*   **Action**: Create a versioned SQL script in `blog-application/src/main/resources/db`.
*   **Naming**: `V{version}__{description}.sql` (e.g., `V1.0.1__create_sys_role.sql`).

### 6.3 Exception Handling
*   **Rule**: Do **NOT** use `try-catch` in Controllers for business logic.
*   **Action**: Throw `com.blog.common.exception.BusinessException` with an `ErrorCode`.
    ```java
    if (user == null) {
        throw new BusinessException(ErrorCode.USER_NOT_FOUND);
    }
    ```

### 6.4 MapStruct Configuration
*   **Rule**: Interfaces **MUST** use `@Mapper(componentModel = "spring")` to enable Spring Injection.

### 6.5 Security Context
*   **Rule**: Do **NOT** pass `userId` as a Controller parameter (security risk).
*   **Action**: Retrieve the current user from the Security Context (e.g., via a `SecurityUtils` helper).

## 6. Base Framework Usage (blog-common/base)

### 6.1 Service Layer Structure
*   **Extend**: `BaseServiceImpl<M, E, V, D, C>` for all Service implementations.
*   **Converter**: Must implement `BaseConverter<D, E, V>` with `@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)`.
*   **DTO Constraint**: All DTOs must implement `Identifiable<T>` interface.

### 6.2 Automatic Validation
*   `saveByDto` and `updateByDto` automatically validate DTOs using JSR-303 annotations.
*   Use `@NotNull`, `@Size`, etc. on DTO fields for validation.
*   Validation failures throw `BusinessException(SystemErrorCode.PARAM_ERROR, ...)`.

### 6.3 Update Operations
*   **Single Update**: `updateByDto` is safe - loads existing entity, applies DTO changes incrementally.
*   **Batch Update**: `updateBatchByDto` is **NOT** safe for partial DTOs - converts DTO to Entity directly without loading existing data.

### 6.4 Delete Operations
*   `removeById` is **idempotent** - returns `true` even if resource doesn't exist (RESTful standard).
*   If you need to verify deletion, check existence with `getById` before calling `removeById`.

### 6.5 Stream Queries
*   `streamVo` requires custom implementation in both Mapper and Service.
*   Mapper: Use `@Options(resultSetType = ResultSetType.FORWARD_ONLY, fetchSize = 1000)`.
*   Service: Override `streamVo` with `@Transactional(readOnly = true)` and use `try-with-resources`.

---
*Reference Documents*:
*   [Architecture Design](ARCHITECTURE_DESIGN.md)
*   [Testing Guide](TESTING_GUIDE.md)
*   [Base Framework Guide](BASE_FRAMEWORK_GUIDE.md)

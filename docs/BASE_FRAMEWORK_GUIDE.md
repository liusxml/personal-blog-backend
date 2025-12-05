# 🛠️ Base Framework 使用指南

`blog-common` 模块提供了一套基于 MyBatis-Plus 的增强型 Base Framework，旨在简化 CRUD 开发，规范代码结构，并提供统一的类型转换机制。

---

## 1. 核心组件

| 组件 | 用途 | 对应层级 |
| :--- | :--- | :--- |
| **`IBaseService<E, V, D>`** | 定义通用的 RESTful 风格业务接口 | Service 接口 |
| **`BaseServiceImpl<M, E, V, D, C>`** | 提供接口的默认实现，集成 MapStruct | Service 实现类 |
| **`BaseConverter<D, E, V>`** | 定义 DTO/Entity/VO 转换契约 | MapStruct 转换器 |
| **`Identifiable<T>`** | 标记 DTO 拥有主键 ID | DTO 类 |

---

## 2. 快速开始 (以 Role 为例)

### 2.1 定义 DTO
DTO 必须实现 `Identifiable` 接口，以便 BaseService 自动提取 ID。

```java
@Data
public class RoleDTO implements Identifiable<Long> {
    private Long id;
    private String roleName;
    // ...
}
```

### 2.2 定义 Converter
继承 `BaseConverter`，并添加 MapStruct 注解。
**关键配置**：必须开启 `nullValuePropertyMappingStrategy = IGNORE`，否则更新时会覆盖旧数据。

```java
@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface RoleConverter extends BaseConverter<RoleDTO, Role, RoleVO> {
    // 可以在此添加自定义的转换方法
}
```

### 2.3 定义 Service 接口
继承 `IBaseService`，指定泛型类型。

```java
public interface IRoleService extends IBaseService<Role, RoleVO, RoleDTO> {
    // 自定义业务方法
    void bindUserToRole(Long roleId, Long userId);
}
```

### 2.4 实现 Service
继承 `BaseServiceImpl`，注入 Mapper 和 Converter。

```java
@Service
@RequiredArgsConstructor
public class RoleServiceImpl extends BaseServiceImpl<RoleMapper, Role, RoleVO, RoleDTO, RoleConverter> implements IRoleService {
    
    // 构造器注入会自动完成，因为父类有带参构造器，且使用了 @RequiredArgsConstructor
    
    @Override
    protected void preSave(Role entity) {
        // 钩子：保存前自动设置默认值
        if (entity.getStatus() == null) {
            entity.setStatus("0");
        }
    }
}
```

### 2.5 使用 Controller
直接调用 Service 的通用方法。

```java
@RestController
@RequestMapping("/system/role")
@RequiredArgsConstructor
public class RoleController {

    private final IRoleService roleService;

    @GetMapping("/{id}")
    public Result<RoleVO> getInfo(@PathVariable Long id) {
        // 自动完成：查库 -> Entity转VO -> 返回
        return roleService.getVoById(id)
                .map(Result::success)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }

    @PostMapping
    public Result<Long> add(@RequestBody RoleDTO roleDto) {
        // 自动完成：DTO转Entity -> 预保存钩子 -> 存库 -> 返回ID
        return Result.success((Long) roleService.saveByDto(roleDto));
    }
    
    @PutMapping
    public Result<Void> edit(@RequestBody RoleDTO roleDto) {
        // 自动完成：查旧数据 -> DTO增量覆盖 -> 预更新钩子 -> 更新库
        roleService.updateByDto(roleDto);
        return Result.success();
    }
}
```

---

## 3. ⚠️ 核心注意事项

### 3.1 自动参数校验
*   **单条更新 (`updateByDto`)**：是安全的。它会先查询旧数据,然后将 DTO 中 **非 null** 的字段覆盖到旧数据上。
*   **批量更新 (`updateBatchByDto`)**：**不安全**！它直接将 DTO 转为 Entity 并调用 MP 的批量更新。如果 DTO 只有部分字段，转换出的 Entity 其他字段为 null。虽然 MP 默认不更新 null 字段，但 `preUpdate` 钩子拿到的对象是不完整的。
*   **自动校验**：`saveByDto` 和 `updateByDto` 会自动调用 JSR-303 校验。如果 DTO 上有 `@NotNull`, `@Size` 等注解，校验失败会抛出 `BusinessException`。

### 3.2 删除操作的幂等性
*   **行为**：`removeById` 遵循 RESTful 幂等性原则。即使资源不存在（删除 0 行），也返回 `true`，表示"资源已不存在"的状态已达成。
*   **影响**：如果您需要明确知道资源是否真的被删除，请在调用前先用 `getById` 检查。

### 3.3 泛型约束
*   `D extends Identifiable`：强制 DTO 必须有 `getId()` 方法。
*   `V`：通常是 VO，也可以是 Entity 本身（如果不想用 VO）。

### 3.4 流式查询 (streamVo)

流式查询适用于处理**大数据量**场景，避免一次性加载所有数据到内存导致 OOM。

#### 为什么需要自定义实现？
`BaseServiceImpl` 中的 `streamVo` 方法默认抛出 `UnsupportedOperationException`，因为流式查询需要 **Mapper 层面的特殊支持**（MyBatis 的 `@Options` 注解配置 `ResultSetType.FORWARD_ONLY` 和 `fetchSize`）。

#### 实现步骤

**1. 在 Mapper 中定义流式查询方法**

```java
@Mapper
public interface UserMapper extends BaseMapper<User> {
    
    /**
     * 流式查询用户列表
     * @param wrapper 查询条件
     * @return 用户流
     */
    @Select("SELECT * FROM sys_user ${ew.customSqlSegment}")
    @Options(resultSetType = ResultSetType.FORWARD_ONLY, fetchSize = 1000)
    Stream<User> streamList(@Param(Constants.WRAPPER) Wrapper<User> wrapper);
}
```

**2. 在 Service 中重写 streamVo 方法**

```java
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends BaseServiceImpl<UserMapper, User, UserVO, UserDTO, UserConverter> 
        implements IUserService {

    @Override
    @Transactional(readOnly = true)
    public void streamVo(Wrapper<User> queryWrapper, StreamProcessor<UserVO> processor) {
        // 使用 try-with-resources 确保 Stream 正确关闭
        try (Stream<User> entityStream = baseMapper.streamList(queryWrapper)) {
            // 将 Entity Stream 转换为 VO Stream 并传递给处理器
            processor.process(entityStream.map(converter::entityToVo));
        }
    }
}
```

**3. 使用示例**

```java
// 导出大量用户数据到 CSV
userService.streamVo(null, voStream -> {
    voStream.forEach(userVO -> {
        // 逐条写入文件，不会一次性加载所有数据到内存
        csvWriter.write(userVO);
    });
});
```

#### ⚠️ 注意事项
*   流式查询必须在 **事务内** 执行（`@Transactional(readOnly = true)`）。
*   `Stream` 必须使用 `try-with-resources` 或手动 `close()`，否则会导致数据库连接泄漏。
*   不要在 Stream 上调用 `collect()` 等终结操作，这会失去流式处理的意义。

### 3.5 扩展性
*   如果通用方法不满足需求（例如需要复杂的关联查询），请在 Service 中自定义方法，不要强行修改 BaseService。

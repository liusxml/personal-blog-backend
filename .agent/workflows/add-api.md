---
description: 如何添加新的 REST API 端点
---

# 添加新 API 端点

## 前置条件
- 确定所属模块（如 system, article）
- 确定资源名称（如 User, Article）
- 确定 API 路径（如 `/api/v1/articles`）

## 操作步骤

### 1. 在 `*-api` 模块创建 DTO/VO

**请求 DTO** (`blog-xxx-api/src/main/java/com/blog/xxx/api/dto/XxxDTO.java`):
```java
@Data
public class XxxDTO implements Serializable, Identifiable<Long> {
    private Long id;
    
    @NotBlank(message = "名称不能为空")
    @Schema(description = "名称", example = "示例")
    private String name;
}
```

**响应 VO** (`blog-xxx-api/src/main/java/com/blog/xxx/api/vo/XxxVO.java`):
```java
@Data
@Schema(description = "Xxx 响应对象")
public class XxxVO {
    @Schema(description = "ID")
    private Long id;
    
    @Schema(description = "名称")
    private String name;
}
```

### 2. 在 `*-service` 模块创建 Converter

```java
@Mapper(componentModel = "spring", 
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface XxxConverter extends BaseConverter<XxxDTO, XxxEntity, XxxVO> {
}
```

### 3. 创建 Service 接口和实现

**接口** (`IXxxService.java`):
```java
public interface IXxxService extends IBaseService<XxxEntity, XxxVO, XxxDTO> {
    // 自定义方法
}
```

**实现** (`XxxServiceImpl.java`):
```java
@Slf4j
@Service
public class XxxServiceImpl 
        extends BaseServiceImpl<XxxMapper, XxxEntity, XxxVO, XxxDTO, XxxConverter>
        implements IXxxService {
    
    public XxxServiceImpl(XxxConverter converter) {
        super(converter);
    }
}
```

### 4. 创建 Controller

```java
@Slf4j
@RestController
@RequestMapping("/api/v1/xxxs")
@RequiredArgsConstructor
@Tag(name = "Xxx管理", description = "Xxx相关接口")
public class XxxController {
    
    private final IXxxService xxxService;
    
    @GetMapping("/{id}")
    @Operation(summary = "获取详情")
    public Result<XxxVO> getById(@PathVariable Long id) {
        return xxxService.getVoById(id)
                .map(Result::success)
                .orElseThrow(() -> new BusinessException(SystemErrorCode.NOT_FOUND));
    }
    
    @PostMapping
    @Operation(summary = "创建")
    public Result<Long> create(@Valid @RequestBody XxxDTO dto) {
        Serializable id = xxxService.saveByDto(dto);
        return Result.success((Long) id);
    }
    
    @PutMapping
    @Operation(summary = "更新")
    public Result<Boolean> update(@Valid @RequestBody XxxDTO dto) {
        return Result.success(xxxService.updateByDto(dto));
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "删除")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(xxxService.removeById(id));
    }
}
```

// turbo
### 5. 验证
```bash
mvn compile -pl blog-modules/blog-module-xxx
```

### 6. 测试 API
启动应用后访问 `http://localhost:8080/swagger-ui.html` 查看新接口

## 检查清单
- [ ] DTO 实现 `Identifiable<T>`
- [ ] VO 添加 `@Schema` 注解
- [ ] Converter 使用正确的 MapStruct 配置
- [ ] Controller 返回 `Result<T>`
- [ ] 接口文档完整

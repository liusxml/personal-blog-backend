---
sidebar_position: 2
---

# ArchUnit 架构测试完整指南

## 📚 目录

1. [什么是 ArchUnit](#什么是-archunit)
2. [为什么使用 ArchUnit](#为什么使用-archunit)
3. [项目中的 ArchUnit 配置](#项目中的-archunit-配置)
4. [规则定义详解](#规则定义详解)
5. [如何运行测试](#如何运行测试)
6. [如何判断测试结果](#如何判断测试结果)
7. [常见违规及修复方法](#常见违规及修复方法)
8. [最佳实践](#最佳实践)

---

## 什么是 ArchUnit

ArchUnit 是一个 **Java 架构测试框架**，它允许你用 **单元测试的方式验证代码架构规则**。

### 核心概念

```java
// ArchUnit 的基本语法
classes()
    .that().resideInAPackage("..controller..")  // 选择器：定义检查哪些类
    .should().beAnnotatedWith(RestController.class)  // 断言：定义期望的行为
    .because("Controllers 必须使用 @RestController 注解");  // 原因：解释为什么
```

**三要素**:
1. **That** - 选择器：定义要检查的类/方法/字段
2. **Should** - 断言：定义这些元素应该满足的条件
3. **Because** - 原因：说明规则的目的（可选但强烈推荐）

---

## 为什么使用 ArchUnit

### 传统问题

❌ **没有 ArchUnit 时**:
- 架构规则只存在于文档中或口头约定
- 依赖 Code Review 人工检查（容易遗漏）
- 架构腐化难以及时发现
- 新成员不了解架构约束

✅ **使用 ArchUnit 后**:
- 架构规则变成自动化测试
- 每次构建自动检查
- 违规立即发现，无法提交
- 规则即文档，清晰明确

### 实际案例

**场景**: 团队约定 "Controller 只能在 service 模块，不能在 application 模块"

```java
// ❌ 错误做法 - 仅靠文档约定
// docs/ARCHITECTURE.md: "Controllers must be in *-service modules"
// 问题：开发者可能不看文档，或者忘记规则

// ✅ 正确做法 - ArchUnit 自动化验证
public static final ArchRule NO_CONTROLLERS_IN_APPLICATION = noClasses()
    .that().areAnnotatedWith(RestController.class)
    .should().resideInAPackage("com.blog.application..")
    .because("Controllers 必须位于 *-service 模块");
```

一旦有人在 `blog-application` 中创建 Controller，**CI/CD 构建会立即失败**。

---

## 项目中的 ArchUnit 配置

### 目录结构

```
blog-application/src/test/java/com/blog/architecture/
├── ArchitectureTest.java              # 全局配置（类加载、缓存）
├── ArchUnitIntegrationTest.java       # 集成测试入口
├── config/
│   └── ArchUnitConfig.java            # 包路径常量配置
└── rules/
    ├── LayerRule.java                 # 分层架构规则
    ├── ModuleRule.java                # 模块依赖规则
    ├── NamingRule.java                # 命名规范规则
    ├── DesignPatternRule.java         # 设计模式规则
    └── ApiRule.java                   # API 规范规则
```

### 核心文件职责

#### 1. `ArchitectureTest.java` - 全局配置

**作用**: 
- 加载并缓存所有待检查的类
- 配置类扫描策略（排除测试代码、生成代码等）

**关键代码**:
```java
@AnalyzeClasses(
    packages = "com.blog",                    // 扫描包
    cacheMode = CacheMode.PER_CLASS,         // 缓存策略
    importOptions = {
        ImportOption.DoNotIncludeTests.class, // 排除测试类
        GeneratedCodeFilter.class             // 排除生成代码
    }
)
public class ArchitectureTest {
    // 缓存的类集合 - 所有规则复用
    public static final JavaClasses CLASSES = ...;
}
```

#### 2. `ArchUnitIntegrationTest.java` - 测试入口

**作用**: 
- 编排所有架构规则的执行
- 按优先级顺序运行测试

**关键代码**:
```java
@DisplayName("✅ ArchUnit 架构规则集成测试套件")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ArchUnitIntegrationTest {
    
    @Test
    @Order(1)
    @DisplayName("1. 验证分层架构规则")
    void testLayerRules() {
        LayerRule.LAYERED_ARCHITECTURE.check(importedClasses);
        LayerRule.CONTROLLERS_IN_CORRECT_PACKAGE.check(importedClasses);
        // ... 更多规则
    }
}
```

#### 3. `*Rule.java` - 规则定义文件

**作用**: 定义具体的架构约束

**示例**: `LayerRule.java`
```java
public final class LayerRule {
    // 规则定义
    public static final ArchRule LAYERED_ARCHITECTURE = layeredArchitecture()
        .layer("Controller").definedBy("..controller..")
        .layer("Service").definedBy("..service..")
        .whereLayer("Controller").mayOnlyBeAccessedByLayers("Controller")
        .whereLayer("Service").mayOnlyBeAccessedByLayers("Controller", "Service");
    
    // 执行入口
    public static void check(JavaClasses classes) {
        LAYERED_ARCHITECTURE.check(classes);
    }
}
```

---

## 规则定义详解

### 规则 1: 分层架构 (LayerRule)

#### 1.1 分层依赖规则

**目的**: 确保分层单向依赖（Controller → Service → Repository → Entity）

```java
public static final ArchRule LAYERED_ARCHITECTURE = layeredArchitecture()
    .consideringAllDependencies()  // 检查所有依赖
    
    // 定义层
    .layer("Controller").definedBy("..controller..")
    .layer("Service").definedBy("..service..")
    .layer("Repository").definedBy("..repository..")
    .layer("Entity").definedBy("..entity..")
    .layer("DTO").definedBy("..dto..")
    
    // 定义访问规则
    .whereLayer("Controller").mayOnlyBeAccessedByLayers("Controller")
    .whereLayer("Service").mayOnlyBeAccessedByLayers("Controller", "Service")
    .whereLayer("Repository").mayOnlyBeAccessedByLayers("Service")
    .whereLayer("Entity").mayOnlyBeAccessedByLayers("Repository", "Service")
    .whereLayer("DTO").mayOnlyBeAccessedByLayers("Controller", "Service", "Repository", "DTO");
```

**违规示例**:
```java
// ❌ 错误：Controller 直接调用 Repository
@RestController
public class UserController {
    @Autowired
    private UserMapper userMapper;  // 违规！应该调用 UserService
}

// ✅ 正确：Controller → Service → Repository
@RestController
public class UserController {
    @Autowired
    private UserService userService;  // 正确
}
```

#### 1.2 Controller 位置规则

**目的**: Controller 必须在 `*-service` 模块，不能在 `blog-application`

```java
public static final ArchRule NO_CONTROLLERS_IN_APPLICATION = noClasses()
    .that().areAnnotatedWith(RestController.class)
    .or().areAnnotatedWith(Controller.class)
    .should().resideInAPackage("com.blog.application..")
    .because("Controllers 必须位于 *-service 模块，blog-application 仅用于启动类和全局配置");
```

**违规检测**:
```java
// ❌ 错误位置
com.blog.application.controller.TestController  // 违规！

// ✅ 正确位置
com.blog.system.service.controller.UserController  // 正确
```

---

### 规则 2: 模块依赖 (ModuleRule)

#### 2.1 跨模块实现依赖检查

**目的**: 模块间不能依赖其他模块的实现（`service.impl`），只能依赖接口（`api`）

```java
public static void checkNoCrossModuleImplDependency() {
    BUSINESS_MODULES.forEach(current -> {
        String currentService = String.format("com.blog.%s.service..", current);
        BUSINESS_MODULES.stream()
            .filter(other -> !other.equals(current))
            .forEach(other -> {
                String otherImpl = String.format("com.blog.%s.service.impl..", other);
                noClasses()
                    .that().resideInAPackage(currentService)
                    .should().dependOnClassesThat().resideInAPackage(otherImpl)
                    .because(String.format("%s 模块不应依赖 %s 的实现层", current, other))
                    .check(CLASSES);
            });
    });
}
```

**示例**:
```java
// ❌ 错误：article 模块直接依赖 system 模块的实现
package com.blog.article.service;
import com.blog.system.service.impl.UserServiceImpl;  // 违规！

// ✅ 正确：通过 API 接口依赖
package com.blog.article.service;
import com.blog.system.api.UserService;  // 正确
```

#### 2.2 API 模块纯度检查

**目的**: `*-api` 模块只能包含 DTO/Interface/Enum，不能有实现类

```java
// 规则 1: 禁止实现类
public static final ArchRule API_MODULE_NO_IMPLEMENTATION = noClasses()
    .that().resideInAPackage("..api..")
    .should().beAnnotatedWith(Service.class)
    .orShould().beAnnotatedWith(Repository.class)
    .orShould().beAnnotatedWith(Component.class)
    .orShould().haveSimpleNameContaining("Impl")
    .because("API 模块禁止包含实现类（仅 DTO/Interface/Enum）");

// 规则 2: 禁止 Entity
public static final ArchRule API_MODULE_NO_ENTITY = noClasses()
    .that().resideInAPackage("..api..")
    .should().beAnnotatedWith(com.baomidou.mybatisplus.annotation.TableName.class)
    .because("API 模块禁止包含 Entity（仅 DTO）");
```

**目录结构要求**:
```
blog-system-api/src/main/java/com/blog/system/api/
├── dto/                      # ✅ 允许：DTO
│   ├── UserDTO.java
│   └── RoleDTO.java
├── service/                  # ✅ 允许：接口
│   └── UserService.java
└── enums/                    # ✅ 允许：枚举
    └── UserStatus.java

blog-system-service/src/main/java/com/blog/system/service/
├── impl/                     # ✅ 正确位置：实现类
│   └── UserServiceImpl.java
├── entity/                   # ✅ 正确位置：Entity
│   └── SysUser.java
└── mapper/                   # ✅ 正确位置：Mapper
    └── UserMapper.java
```

---

### 规则 3: 命名规范 (NamingRule)

#### 3.1 DTO 必须实现 Serializable

**目的**: 支持缓存和分布式传输

```java
classes().that().resideInAPackage("..dto..")
    .and().haveSimpleNameEndingWith("DTO")
    .should().implement(Serializable.class)
    .because("DTOs 必须实现 Serializable 以支持缓存和分布式传输");
```

**修复方法**:
```java
// ❌ 错误
public class UserDTO {
    private Long id;
    private String username;
}

// ✅ 正确
import java.io.Serializable;

public class UserDTO implements Serializable {
    private Long id;
    private String username;
}
```

#### 3.2 Entity 必须有 @TableName 注解

**目的**: 确保 Entity 正确映射数据库表

```java
classes().that().resideInAPackage("..entity..")
    .and().areNotInterfaces()
    .and().areNotEnums()
    .should().beAnnotatedWith(com.baomidou.mybatisplus.annotation.TableName.class)
    .because("Entity 必须使用 @TableName 注解映射数据库表");
```

**修复方法**:
```java
// ❌ 错误
public class SysUser {
    private Long id;
}

// ✅ 正确
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("sys_user")
public class SysUser {
    private Long id;
}
```

#### 3.3 MapStruct Converter 命名规范

**目的**: 统一 MapStruct 转换器的命名和位置

```java
classes().that().areAnnotatedWith(org.mapstruct.Mapper.class)
    .should().resideInAnyPackage("..mapper..", "..converter..")
    .andShould().haveSimpleNameEndingWith("Mapper")
    .orShould().haveSimpleNameEndingWith("Converter")
    .check(ArchitectureTest.CLASSES);
```

**示例**:
```java
// ✅ 正确 - 方式 1
package com.blog.system.mapper;
@Mapper(componentModel = "spring")
public interface UserMapper { }

// ✅ 正确 - 方式 2
package com.blog.system.converter;
@Mapper(componentModel = "spring")
public interface UserConverter { }

// ❌ 错误
package com.blog.system.util;  // 错误的包
@Mapper
public interface UserTool { }  // 错误的命名
```

---

### 规则 4: 设计模式 (DesignPatternRule)

#### 4.1 禁止字段注入

**目的**: 提升可测试性，使用构造器注入

```java
public static final ArchRule NO_FIELD_INJECTION = noFields()
    .should().beAnnotatedWith(org.springframework.beans.factory.annotation.Autowired.class)
    .because("禁止使用字段注入，应使用构造器注入（@RequiredArgsConstructor）");
```

**修复方法**:
```java
// ❌ 错误：字段注入
@Service
public class UserService {
    @Autowired  // 违规！
    private UserMapper userMapper;
}

// ✅ 正确：构造器注入
@Service
@RequiredArgsConstructor  // Lombok 自动生成构造器
public class UserService {
    private final UserMapper userMapper;  // final + 构造器注入
}
```

#### 4.2 Service 实现应继承 BaseServiceImpl

**目的**: 复用 CRUD 能力，减少重复代码

```java
public static final ArchRule SERVICE_IMPL_SHOULD_EXTEND_BASE = classes()
    .that().resideInAPackage("..service.impl..")
    .and().areNotInterfaces()
    .and().haveSimpleNameEndingWith("ServiceImpl")
    .and().doNotHaveSimpleName("DBUserDetailsServiceImpl")  // 豁免 Spring Security
    .should().beAssignableTo(com.blog.common.base.BaseServiceImpl.class)
    .because("标准 Service 实现应继承 BaseServiceImpl 以复用 CRUD（特殊服务除外）");
```

**示例**:
```java
// ❌ 错误：未继承 BaseServiceImpl
public class UserServiceImpl implements UserService {
    // 需要手动实现所有 CRUD 方法...
}

// ✅ 正确：继承 BaseServiceImpl
public class UserServiceImpl 
    extends BaseServiceImpl<UserMapper, SysUser, UserVO, UserDTO, UserConverter>
    implements UserService {
    // 自动获得 CRUD 方法，只需实现业务逻辑
}
```

---

### 规则 5: API 规范 (ApiRule)

#### 5.1 Controller 必须返回 Result&lt;T&gt;

**目的**: 统一 API 响应格式

```java
public static final ArchRule CONTROLLER_MUST_RETURN_RESULT = methods()
    .that().areDeclaredInClassesThat().areAnnotatedWith(RestController.class)
    .and().arePublic()
    .and().areNotAnnotatedWith(ExceptionHandler.class)  // 豁免异常处理
    .and().doNotHaveName("download.*")                  // 豁免下载
    .and().doNotHaveName("upload.*")                    // 豁免上传
    .should(new ArchCondition<JavaMethod>("return Result&lt;T&gt;") {
        @Override
        public void check(JavaMethod method, ConditionEvents events) {
            JavaClass returnType = method.getRawReturnType();
            // 豁免特殊类型
            if (returnType.isAssignableTo(ResponseEntity.class) ||
                returnType.isAssignableTo(SseEmitter.class) ||
                returnType.getName().equals("void")) {
                return;
            }
            // 检查是否返回 Result
            if (!returnType.isAssignableTo(Result.class)) {
                events.add(SimpleConditionEvent.violated(method, 
                    "Method should return Result&lt;T&gt;"));
            }
        }
    });
```

**修复方法**:
```java
// ❌ 错误：直接返回 DTO
@GetMapping("/{id}")
public UserDTO getUser(@PathVariable Long id) {
    return userService.getById(id);  // 违规！
}

// ✅ 正确：返回 Result<UserDTO>
@GetMapping("/{id}")
public Result<UserDTO> getUser(@PathVariable Long id) {
    UserDTO user = userService.getById(id);
    return Result.success(user);  // 统一响应格式
}

// ✅ 豁免：文件下载
@GetMapping("/download/{id}")
public ResponseEntity<Resource> downloadFile(@PathVariable Long id) {
    // 允许返回 ResponseEntity
}
```

#### 5.2 Controller 禁止返回 Entity

**目的**: 保护内部数据结构，避免过度暴露

```java
public static final ArchRule CONTROLLER_NO_ENTITY_IN_RESPONSE = methods()
    .that().areDeclaredInClassesThat().areAnnotatedWith(RestController.class)
    .and().arePublic()
    .should(new ArchCondition<JavaMethod>("not return Entity") {
        @Override
        public void check(JavaMethod method, ConditionEvents events) {
            JavaClass returnType = method.getRawReturnType();
            String returnTypeName = returnType.getName();
            boolean isEntity = returnTypeName.contains(".entity.") ||
                             returnTypeName.endsWith("Entity");
            if (isEntity) {
                events.add(SimpleConditionEvent.violated(method, 
                    "Controller should not return Entity directly"));
            }
        }
    });
```

**修复方法**:
```java
// ❌ 错误：直接返回 Entity
@GetMapping("/{id}")
public Result<SysUser> getUser(@PathVariable Long id) {
    SysUser user = userMapper.selectById(id);
    return Result.success(user);  // 违规：暴露了内部 Entity
}

// ✅ 正确：返回 DTO
@GetMapping("/{id}")
public Result<UserDTO> getUser(@PathVariable Long id) {
    UserDTO userDto = userService.getDtoById(id);  // 通过 MapStruct 转换
    return Result.success(userDto);
}
```

---

## 如何运行测试

### 方式 1: Maven 命令行

#### 运行所有 ArchUnit 测试
```bash
# 完整命令
mvn test -Dtest=ArchUnitIntegrationTest -pl blog-application

# 带详细输出
mvn test -Dtest=ArchUnitIntegrationTest -pl blog-application -X
```

#### 运行特定测试方法
```bash
# JUnit 5 方式（需要 surefire 3.0+）
mvn test -Dtest=ArchUnitIntegrationTest#testLayerRules -pl blog-application
```

#### 在 CI/CD 中运行
```bash
# 完整构建（包含所有测试）
mvn clean verify

# 仅运行测试（跳过编译）
mvn test
```

### 方式 2: IDE 运行

#### IntelliJ IDEA
1. 打开 `ArchUnitIntegrationTest.java`
2. 点击类名旁的绿色箭头 → **Run 'ArchUnitIntegrationTest'**
3. 或右键单个测试方法 → **Run 'testLayerRules()'**

#### Eclipse
1. 右键 `ArchUnitIntegrationTest.java`
2. **Run As** → **JUnit Test**

### 方式 3: Gradle（如果使用）
```bash
./gradlew test --tests ArchUnitIntegrationTest
```

---

## 如何判断测试结果

### 测试成功的标志

#### 1. 命令行输出（Maven）
```
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.blog.architecture.ArchUnitIntegrationTest
22:10:00.123 [main] INFO  ArchUnitIntegrationTest - ▶️ ArchUnit 测试初始化: 正在加载待检查的 Java 类...
22:10:02.456 [main] INFO  ArchUnitIntegrationTest - ✅ ArchUnit 测试初始化完成: 共加载 1234 个类进行架构检查。
22:10:02.500 [main] INFO  ArchUnitIntegrationTest - ▶️ 开始测试 (1/4): 分层架构规则...
22:10:03.100 [main] INFO  ArchUnitIntegrationTest - ✅ 测试通过 (1/4): 分层架构规则全部遵守。
22:10:03.150 [main] INFO  ArchUnitIntegrationTest - ▶️ 开始测试 (2/4): 模块间依赖规则...
22:10:04.200 [main] INFO  ArchUnitIntegrationTest - ✅ 测试通过 (2/4): 模块间依赖规则全部遵守。
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 5.123 s
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS  ✅
[INFO] ------------------------------------------------------------------------
```

**关键指标**:
- ✅ `BUILD SUCCESS`
- ✅ `Failures: 0, Errors: 0`
- ✅ 所有测试方法输出 "✅ 测试通过"

#### 2. IDE 输出
![IntelliJ IDEA 成功](https://img.shields.io/badge/Tests-6%20passed-brightgreen?style=for-the-badge)

**标志**:
- ✅ 绿色勾号
- ✅ "All tests passed"
- ✅ 测试执行时间显示

---

### 测试失败的标志

#### 1. 命令行输出（Maven）
```
[ERROR] Tests run: 6, Failures: 2, Errors: 0, Skipped: 0, Time elapsed: 5.123 s <<< FAILURE!
[ERROR] testNamingRules  Time elapsed: 0.823 s  <<< FAILURE!
java.lang.AssertionError: Architecture Violation [Priority: MEDIUM] - Rule 'classes that reside in a package '..dto..' and have simple name ending with 'DTO' should implement java.io.Serializable' was violated (2 times):

Class <com.blog.frameworktest.dto.TestDTO> does not implement java.io.Serializable in (TestDTO.java:0)
Class <com.blog.frameworktest.dto.ValidationTestDTO> does not implement java.io.Serializable in (ValidationTestDTO.java:0)

[ERROR]   ArchUnitIntegrationTest.testNamingRules:99 Architecture Violation [Priority: MEDIUM] - Rule '...' was violated (2 times):
...

[INFO] ------------------------------------------------------------------------
[INFO] BUILD FAILURE  ❌
[INFO] ------------------------------------------------------------------------
```

**关键指标**:
- ❌ `BUILD FAILURE`
- ❌ `Failures: 2` (大于 0)
- ❌ `Architecture Violation` 字样
- ❌ 具体违规类和文件位置

#### 2. IDE 输出
![IntelliJ IDEA 失败](https://img.shields.io/badge/Tests-2%20failed-red?style=for-the-badge)

**标志**:
- ❌ 红色叉号
- ❌ "X tests failed"
- ❌ 可点击查看详细错误

---

### 理解错误消息

#### 错误消息结构
```
Architecture Violation [Priority: MEDIUM] - Rule '规则描述' was violated (违规次数 times):

违规详情 1
违规详情 2
...

[ERROR] TestClass.testMethod:行号 Architecture Violation...
```

#### 示例 1: DTO Serializable 违规

**错误消息**:
```
Rule 'classes that reside in a package '..dto..' and have simple name ending with 'DTO' 
should implement java.io.Serializable, because DTOs 必须实现 Serializable 以支持缓存和分布式传输' 
was violated (2 times):

Class <com.blog.frameworktest.dto.TestDTO> does not implement java.io.Serializable in (TestDTO.java:0)
Class <com.blog.frameworktest.dto.ValidationTestDTO> does not implement java.io.Serializable in (ValidationTestDTO.java:0)
```

**解读**:
- **规则**: DTO 必须实现 Serializable
- **原因**: 支持缓存和分布式传输
- **违规**: 2 个类未实现
- **位置**: `TestDTO.java`, `ValidationTestDTO.java`

**修复步骤**:
1. 打开 `TestDTO.java`
2. 添加 `implements Serializable`
3. 导入 `java.io.Serializable`
4. 重新运行测试

#### 示例 2: Controller 返回类型违规

**错误消息**:
```
Controller method UserController.getUser() returns 'UserDTO' instead of Result&lt;T&gt;. 
SOLUTION: Change return type to Result<UserDTO> and wrap response with Result.success().
```

**解读**:
- **违规方法**: `UserController.getUser()`
- **问题**: 返回 `UserDTO` 而非 `Result<UserDTO>`
- **修复建议**: 明确告知如何修复

**修复步骤**:
1. 打开 `UserController.java`
2. 找到 `getUser()` 方法
3. 修改返回类型为 `Result<UserDTO>`
4. 使用 `Result.success(userDto)` 包装返回值

---

## 常见违规及修复方法

### 违规 1: Controller 在错误的模块

**错误**:
```
Class <com.blog.application.controller.TestController> should not reside in package 'com.blog.application..'
```

**修复**:
```bash
# 移动文件到正确的模块
mv blog-application/src/main/java/com/blog/application/controller/TestController.java \
   blog-system-service/src/main/java/com/blog/system/service/controller/TestController.java
```

---

### 违规 2: DTO 缺少 Serializable

**错误**:
```
Class <com.blog.system.api.dto.UserDTO> does not implement java.io.Serializable
```

**修复**:
```java
// 1. 添加 import
import java.io.Serializable;

// 2. 实现接口
public class UserDTO implements Serializable {
    // ... 字段
}
```

---

### 违规 3: 使用字段注入

**错误**:
```
Field <com.blog.system.service.impl.UserServiceImpl.userMapper> is annotated with @Autowired
```

**修复**:
```java
// 修改前
@Service
public class UserServiceImpl {
    @Autowired
    private UserMapper userMapper;
}

// 修改后
@Service
@RequiredArgsConstructor
public class UserServiceImpl {
    private final UserMapper userMapper;  // 构造器注入
}
```

---

### 违规 4: Service 未继承 BaseServiceImpl

**错误**:
```
Class <com.blog.system.service.impl.UserServiceImpl> is not assignable to BaseServiceImpl
```

**修复**:
```java
// 修改前
public class UserServiceImpl implements UserService {
    // ...
}

// 修改后
public class UserServiceImpl 
    extends BaseServiceImpl<UserMapper, SysUser, UserVO, UserDTO, UserConverter>
    implements UserService {
    // 自动获得 CRUD 能力
}
```

---

### 违规 5: API 模块包含实现类

**错误**:
```
Class <com.blog.system.api.impl.UserServiceImpl> should not reside in package '..api..'
```

**修复**:
```bash
# 移动实现类到 service 模块
mv blog-system-api/src/main/java/com/blog/system/api/impl/UserServiceImpl.java \
   blog-system-service/src/main/java/com/blog/system/service/impl/UserServiceImpl.java
```

---

## 最佳实践

### 1. 在 CI/CD 中强制执行

**GitHub Actions 示例**:
```yaml
name: Build
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Set up JDK 21
        uses: actions/setup-java@v2
        with:
          java-version: '21'
      - name: Run ArchUnit Tests
        run: mvn test -Dtest=ArchUnitIntegrationTest
```

### 2. 定期 Review 规则

**建议频率**: 每季度 review 一次

**Review 内容**:
- 豁免列表是否合理
- 是否有新的架构约束需要添加
- 规则描述是否清晰

### 3. 新规则先警告后强制

**渐进式引入**:
```java
// 第1阶段：警告（不阻止构建）
@Test
@Disabled("警告阶段：仅记录违规，不阻止构建")
void testNewRule() {
    // ... 规则检查
}

// 第2阶段（2周后）：强制执行
@Test
void testNewRule() {
    NEW_RULE.check(importedClasses);  // 违规会导致构建失败
}
```

### 4. 提供清晰的错误消息

**好的错误消息**:
```java
.because("Controller 禁止直接返回 Entity。" +
         "SOLUTION: 创建对应的 DTO 并使用 MapStruct 转换。" +
         "示例：UserDTO userDto = userConverter.toDto(userEntity);");
```

**不好的错误消息**:
```java
.because("违反规则");  // ❌ 太模糊
```

### 5. 使用分类和优先级

**规则分类**:
- **P0 (Critical)**: 架构核心约束，必须遵守
- **P1 (High)**: 重要规范，强烈建议
- **P2 (Medium)**: 最佳实践，建议遵守
- **P3 (Low)**: 代码风格，可选

**测试顺序**:
```java
@Order(1) void testCriticalRules() { }  // P0
@Order(2) void testImportantRules() { }  // P1
@Order(3) void testBestPractices() { }  // P2
```

---

## 故障排除

### 问题 1: 构建太慢

**症状**: ArchUnit 测试耗时超过 30 秒

**原因**: 类扫描范围太大

**解决**:
```java
// 缩小扫描范围
@AnalyzeClasses(
    packages = "com.blog",  // 改为具体子包
    cacheMode = CacheMode.PER_CLASS  // 启用缓存
)
```

### 问题 2: 测试不稳定

**症状**: 有时通过有时失败

**原因**: 规则定义太宽泛

**解决**:
```java
// 添加更精确的条件
classes()
    .that().resideInAPackage("..service..")
    .and().areNotInterfaces()  // 添加条件
    .and().areNotAnnotations()  // 添加条件
    .should()...
```

### 问题 3: 误报太多

**症状**: 大量合理的代码被标记为违规

**原因**: 规则太严格或缺少豁免

**解决**:
```java
// 添加豁免条件
classes()
    .that().resideInAPackage("..service..")
    .and().doNotHaveSimpleName("SpecialService")  // 豁免特殊类
    .should()...
```

---

## 扩展阅读

### 官方文档
- [ArchUnit 用户指南](https://www.archunit.org/userguide/html/000_Index.html)
- [ArchUnit API 文档](https://javadoc.io/doc/com.tngtech.archunit/archunit/latest/index.html)

### 推荐文章
- [架构测试实践](https://www.thoughtworks.com/insights/blog/architecture-testing)
- [ArchUnit 最佳实践](https://www.baeldung.com/java-archunit)

### 示例项目
- [Spring PetClinic ArchUnit](https://github.com/spring-projects/spring-petclinic)
- [DDD Sample App](https://github.com/citerus/dddsample-core)

---

## 总结

ArchUnit 是维护代码架构的**自动化卫士**：

✅ **自动化**: 每次构建自动检查，无需人工  
✅ **可见性**: 清晰的错误提示，快速定位问题  
✅ **文档化**: 规则即文档，一目了然  
✅ **预防性**: 在问题发生前就阻止违规代码

**记住**: 架构规则不是束缚，而是保护项目长期健康发展的护栏！

---
description: 如何运行项目测试套件
---

# 运行测试

## 快速命令

// turbo-all

### 运行所有测试
```bash
mvn test
```

### 运行单个模块测试
```bash
# 系统模块测试
mvn test -pl blog-modules/blog-module-system/blog-system-service

# 应用模块测试（含架构测试）
mvn test -pl blog-application
```

### 运行架构测试
```bash
mvn test -pl blog-application -Dtest=ArchitectureTest
```

### 运行特定测试类
```bash
mvn test -pl blog-modules/blog-module-system/blog-system-service -Dtest=AuthControllerTest
```

### 运行特定测试方法
```bash
mvn test -pl blog-modules/blog-module-system/blog-system-service \
    -Dtest=AuthControllerTest#should_returnToken_when_loginWithValidCredentials
```

### 跳过测试构建
```bash
mvn clean install -DskipTests
```

### 生成测试报告
```bash
mvn test jacoco:report
# 报告位置: target/site/jacoco/index.html
```

## 测试分类

| 类型 | 位置 | 命令 |
|:---|:---|:---|
| 单元测试 | `*-service/src/test` | `mvn test` |
| 集成测试 | `blog-application/src/test` | `mvn verify` |
| 架构测试 | `blog-application/src/test/architecture` | `-Dtest=ArchitectureTest` |

## 常用断言（AssertJ）

```java
// 基本断言
assertThat(result).isNotNull();
assertThat(result.code()).isEqualTo(0);

// 集合断言
assertThat(list).hasSize(3).contains(item);

// 异常断言
assertThatThrownBy(() -> service.method())
    .isInstanceOf(BusinessException.class);
```

## 注意事项
- 测试数据库使用 H2 内存数据库（如已配置）
- 集成测试需要 Redis（或使用 embedded-redis）
- 架构测试会验证模块依赖规则

---
sidebar_position: 3
---

# ⚠️ @MockBean Deprecation 说明

## 问题描述

在 Spring Boot 3.4.0+ 中，`@MockBean` 和 `@SpyBean` 被标记为 **deprecated**，计划在 Spring Boot 4.0.0 中移除。

## 为什么被弃用？

Spring 团队希望标准化并增强与 Mockito 框架的集成。`@MockBean` 是 Spring Boot 特有的扩展，现在推荐使用更标准的注解。

## 迁移方案

### 当前状态 (Spring Boot 3.5.7)

```java
@SpringBootTest
class FileStorageIntegrationTest {
    
    @SuppressWarnings("deprecation")
    @MockBean
    private S3Client s3Client;  // ✅ 仍然可用，功能正常
    
    @SuppressWarnings("deprecation")
    @MockBean
    private S3Presigner s3Presigner;
}
```

**为什么继续使用 `@MockBean`:**
- ✅ Spring Boot 4.0 之前都可用
- ✅ 功能完全正常
- ✅ `@MockitoBean` 在 Spring Boot 3.5.7 中可能尚未完全实现
- ✅ 使用 `@SuppressWarnings("deprecation")` 消除警告

### 未来迁移 (Spring Boot 4.0+)

```java
@SpringBootTest
class FileStorageIntegrationTest {
    
    // 方案1: 使用 @MockitoBean (官方推荐)
    @MockitoBean
    private S3Client s3Client;
    
    @MockitoBean
    private S3Presigner s3Presigner;
    
    // 方案2: 使用 @TestBean (Spring Framework 6.2+)
    // 用于更通用的 Bean 覆盖场景
    @TestBean
    static S3Client s3Client() {
        return mock(S3Client.class);
    }
}
```

## 注意事项

### @MockitoBean 的限制

⚠️ **重要**: `@MockitoBean` 不是 `@MockBean` 的完全替代品:

1. **不支持在 @Configuration 或 @Component 上使用**
   ```java
   // ❌ 不支持
   @TestConfiguration
   class TestConfig {
       @MockitoBean
       private S3Client s3Client;  // 不工作
   }
   ```

2. **可能缺少某些 @MockBean 的功能**
   - Spring 团队正在持续改进
   - 建议等待 Spring Boot 3.6 或 4.0 正式版

### 迁移时间表

| Spring Boot 版本 | @MockBean 状态 | 建议操作 |
|-----------------|----------------|---------|
| 3.4.0 - 3.5.x | Deprecated | 继续使用，添加 @SuppressWarnings |
| 3.6.x | Deprecated | 评估 @MockitoBean 稳定性 |
| 4.0.0+ | 移除 | 必须迁移到 @MockitoBean |

## 最佳实践

### 现在（2025年）

```java
// ✅ 推荐: 继续使用 @MockBean，添加抑制警告和注释
@SpringBootTest
class MyTest {
    
    @SuppressWarnings("deprecation")  // 抑制警告
    @MockBean  // TODO: 升级到 Spring Boot 4.0+ 时迁移到 @MockitoBean
    private SomeService service;
}
```

### 准备迁移

1. **监控 Spring Boot 更新**
   - 关注 Spring Boot 3.6.x 的发布说明
   - 查看 @MockitoBean 的完整功能列表

2. **测试迁移**
   - 在非关键测试中尝试 @MockitoBean
   - 验证所有功能正常工作

3. **批量迁移**
   - 升级到 Spring Boot 4.0 之前
   - 一次性替换所有 @MockBean → @MockitoBean

## 参考资源

- [Spring Boot Release Notes](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-3.4-Release-Notes)
- [Spring Framework 6.2 @TestBean](https://docs.spring.io/spring-framework/reference/testing/testcontext-framework/bean-overriding.html)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/)

## 总结

### 当前项目

- ✅ 使用 `@MockBean` + `@SuppressWarnings("deprecation")`
- ✅ 添加 TODO 注释提醒未来迁移
- ✅ 所有测试正常通过
- ✅ 等待 Spring Boot 4.0 正式版发布后再迁移

### 行动计划

1. **短期 (现在)**: 继续使用 @MockBean
2. **中期 (Spring Boot 3.6)**: 评估 @MockitoBean
3. **长期 (Spring Boot 4.0)**: 强制迁移到 @MockitoBean

---

**最后更新**: 2025-11-25  
**项目版本**: Spring Boot 3.5.7

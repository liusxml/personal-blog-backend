# MetaObjectHandler Bean冲突问题

## 问题描述

在集成MyBatis-Plus优化时，应用启动失败，报错：

```
expected single matching bean but found 2: myMetaObjectHandler,metaObjectHandler
```

## 错误信息

```
Error creating bean with name 'sqlSessionFactory': 
No qualifying bean of type 'com.baomidou.mybatisplus.core.handlers.MetaObjectHandler' available: 
expected single matching bean but found 2: myMetaObjectHandler,metaObjectHandler
```

## 根本原因

**MetaObjectHandler被重复注册为Spring Bean**

存在两个Bean定义：

1. `MyMetaObjectHandler`类上的`@Component`注解
   ```java
   @Component  // ❌ 导致重复
   public class MyMetaObjectHandler implements MetaObjectHandler {
   ```

2. `MybatisPlusHandlerConfig`中的`@Bean`方法
   ```java
   @Bean
   public MetaObjectHandler metaObjectHandler() {
       return new MyMetaObjectHandler();  // ❌ 导致重复
   }
   ```

Spring容器中同时存在：
- `myMetaObjectHandler` (来自@Component)
- `metaObjectHandler` (来自@Bean)

导致MyBatis-Plus在注入时不知道该使用哪个Bean。

---

## 解决方案

### 方案1: 移除@Component注解（推荐）⭐⭐⭐⭐⭐

**保留配置类中的@Bean定义，移除实现类上的@Component注解**

```java
// MyMetaObjectHandler.java
@Slf4j
// @Component  ← 移除此注解
public class MyMetaObjectHandler implements MetaObjectHandler {
    // ...
}
```

```java
// MybatisPlusHandlerConfig.java
@Configuration
public class MybatisPlusHandlerConfig {
    
    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MyMetaObjectHandler();  // ✅ 保留
    }
}
```

**优点**:
- ✅ 配置集中管理（所有Bean定义在Config类中）
- ✅ 符合Spring Boot最佳实践
- ✅ 易于切换实现（只需修改Config类）

---

### 方案2: 移除@Bean方法

**保留实现类上的@Component注解，移除配置类中的@Bean定义**

```java
// MyMetaObjectHandler.java
@Slf4j
@Component  // ✅ 保留
public class MyMetaObjectHandler implements MetaObjectHandler {
    // ...
}
```

```java
// MybatisPlusHandlerConfig.java
@Configuration
public class MybatisPlusHandlerConfig {
    
    // @Bean
    // public MetaObjectHandler metaObjectHandler() {  ← 移除整个方法
    //     return new MyMetaObjectHandler();
    // }
}
```

**优点**:
- ✅ 代码更简洁
- ✅ 自动组件扫描

---

### 方案3: 使用@Primary标记主Bean

**当需要保留两个Bean时，标记其中一个为主要Bean**

```java
@Configuration
public class MybatisPlusHandlerConfig {
    
    @Bean
    @Primary  // 标记为主要Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MyMetaObjectHandler();
    }
}
```

**仅适用于**: 需要保留多个实现用于测试或A/B切换的场景

---

## 预防措施

### 1. 代码审查检查点

在添加MyBatis-Plus自定义Handler时检查：

- ✅ 是否已在配置类中定义Bean？
- ✅ 实现类是否添加了@Component注解？
- ✅ 是否有重复注册？

### 2. IDEA提示

启用IDEA的Bean重复检测：

**Settings** → **Editor** → **Inspections** → **Spring** → **Core** → **Incorrect bean definition**

### 3. 单元测试

编写测试验证Bean唯一性：

```java
@SpringBootTest
class MetaObjectHandlerTest {
    
    @Autowired
    private ApplicationContext context;
    
    @Test
    void shouldHaveOnlyOneMetaObjectHandler() {
        Map<String, MetaObjectHandler> beans = 
            context.getBeansOfType(MetaObjectHandler.class);
        
        assertThat(beans).hasSize(1)
            .containsKey("metaObjectHandler");
    }
}
```

---

## 相关问题

### Spring Bean命名规则

- **@Component**: Bean名称 = 类名首字母小写
  - `MyMetaObjectHandler` → `myMetaObjectHandler`

- **@Bean**: Bean名称 = 方法名
  - `metaObjectHandler()` → `metaObjectHandler`

### 常见Bean冲突场景

1. **Converter重复注册** (MapStruct)
   - 接口上`@Mapper(componentModel = "spring")`
   - 配置类中手动`@Bean`

2. **Filter重复注册**
   - 类上`@Component`
   - `FilterRegistrationBean`手动注册

3. **Interceptor重复注册**
   - 类上`@Component`
   - `addInterceptors()`中手动注册

---

## 经验教训

### 1. 渐进式修改

**反面案例**: 一次性修改多个文件导致难以定位问题

**推荐做法**: 分步实施，每步测试
```
1. 修改MyMetaObjectHandler → 测试启动 ✅
2. 添加Entity注解 → 测试启动 ✅
3. 修改主键策略 → 测试启动 ✅
4. 添加p6spy → 测试启动 ✅
```

### 2. Git分支策略

每个优化使用独立分支：

```bash
git checkout -b optimize/mybatis-plus-handler
# 修改后测试
git commit -m "优化MyMetaObjectHandler"

git checkout -b optimize/entity-annotations
# 修改后测试
git commit -m "添加Entity自动填充注解"
```

### 3. 回滚策略

**保持main分支可用**，出问题立即回退：

```bash
# 遇到问题
git checkout -- .  # 回退所有未提交改动
mvn clean install -DskipTests
# 重新启动验证
```

---

## 参考资源

- [Spring Framework Reference - Bean Definition](https://docs.spring.io/spring-framework/reference/core/beans/definition.html)
- [MyBatis-Plus自定义MetaObjectHandler](https://baomidou.com/guide/auto-fill-metainfo.html)
- [Spring Boot Auto-configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/using.html#using.auto-configuration)

---

**最后更新**: 2025-12-11  
**相关文档**: [MyBatis-Plus使用指南](/infrastructure/mybatis-plus)

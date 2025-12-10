---
sidebar_position: 8
---

# p6spy SQL监控

本项目集成 **p6spy** 用于SQL性能监控和日志记录，仅在开发环境启用。

## 🎯 什么是p6spy

**p6spy** 是一个开源的SQL拦截和日志框架，它通过JDBC驱动代理的方式，拦截所有数据库操作，记录完整的SQL语句和执行时间。

### 核心特性

- ✅ **SQL可视化**: 显示完整SQL语句（含参数替换）
- ✅ **性能监控**: 记录每条SQL的执行时间
- ✅ **慢SQL检测**: 自动标记超时SQL
- ✅ **零侵入**: 无需修改业务代码
- ✅ **灵活配置**: 支持多种输出方式和过滤规则

## 📦 项目集成

### 1. 依赖配置

```xml title="blog-application/pom.xml"
<dependency>
    <groupId>p6spy</groupId>
    <artifactId>p6spy</artifactId>
    <version>3.9.1</version>
</dependency>
```

### 2. 数据源配置

```yaml title="application-dev.yaml"
spring:
  datasource:
    driver-class-name: com.p6spy.engine.spy.P6SpyDriver  # 使用p6spy代理驱动
    url: jdbc:p6spy:mysql://localhost:3306/blog_db      # URL前缀改为p6spy

logging:
  level:
    p6spy: INFO  # p6spy日志级别
```

**关键配置说明**:
- `driver-class-name`: 从 `com.mysql.cj.jdbc.Driver` 改为 `com.p6spy.engine.spy.P6SpyDriver`
- `url`: 在JDBC URL前添加 `p6spy:` 前缀

### 3. spy.properties配置

在 `src/main/resources/` 目录下创建 `spy.properties`：

```properties title="blog-application/src/main/resources/spy.properties"
# ============================================================================
# p6spy SQL性能监控配置（基于MyBatis-Plus官方示例）
# ============================================================================

# ============================================================================
# 模块配置（使用MyBatis-Plus专用模块）
# ============================================================================
modulelist=com.baomidou.mybatisplus.extension.p6spy.MybatisPlusLogFactory,com.p6spy.engine.outage.P6OutageFactory

# ============================================================================
# 日志配置
# ============================================================================
# 自定义日志打印格式（MyBatis-Plus格式化器）
logMessageFormat=com.baomidou.mybatisplus.extension.p6spy.P6SpyLogger

# 日志输出到控制台（开发环境推荐）
appender=com.baomidou.mybatisplus.extension.p6spy.StdoutLogger

# 或使用Slf4j记录SQL（输出到日志文件）
#appender=com.p6spy.engine.spy.appender.Slf4JLogger

# ============================================================================
# 慢SQL检测
# ============================================================================
# 是否开启慢SQL记录
outagedetection=true

# 慢SQL记录标准：2秒
outagedetectioninterval=2

# ============================================================================
# 过滤配置  
# ============================================================================
# 排除的分类（只排除噪音日志，保留SQL日志）
# 可选值：error,info,batch,debug,statement,commit,rollback,result,resultset
excludecategories=info,debug,result,commit,resultset

# ============================================================================
# 其他配置
# ============================================================================
# 日期格式
dateformat=yyyy-MM-dd HH:mm:ss

# 取消JDBC URL前缀
useprefix=true

# 设置p6spy driver代理
deregisterdrivers=true
```

## 📊 SQL日志示例

### 控制台输出

启动应用后，调用接口会在控制台看到：

```log
 Consume Time：31 ms 2025-12-11 06:29:59
 Execute SQL：SELECT id,username,nickname,password,email,avatar,status,version,
              create_by,create_time,update_by,update_time,is_deleted 
              FROM sys_user 
              WHERE username = 'admin' AND is_deleted = 0

 Consume Time：4 ms 2025-12-11 06:29:59
 Execute SQL：SELECT r.id AS role_id, r.role_name AS role_name, r.role_key AS role_key 
              FROM sys_role r 
              INNER JOIN sys_user_role ur ON r.id = ur.role_id 
              WHERE ur.user_id = 1
```

### 日志文件输出

如果配置了Slf4j appender，SQL日志会同时输出到日志文件：

```log title="logs/personal-blog-backend.log"
2025-12-11 06:29:59.495 INFO  [http-nio-8080-exec-6] [/] p6spy:  Consume Time：31 ms 2025-12-11 06:29:59
 Execute SQL：SELECT id,username,nickname,password,email,avatar,status,version,create_by,create_time,update_by,update_time,is_deleted FROM sys_user WHERE username = 'admin' AND is_deleted = 0
```

## ⚙️ 配置选项详解

### appender (日志输出方式)

| Appender | 说明 | 适用场景 |
|----------|------|----------|
| `StdoutLogger` | 输出到控制台 | ✅ 开发环境实时查看 |
| `Slf4JLogger` | 输出到日志文件 | ✅ 需要持久化日志 |
| `FileLogger` | 输出到独立文件 | 较少使用 |

### modulelist (模块列表)

```properties
# MyBatis-Plus专用（推荐）
modulelist=com.baomidou.mybatisplus.extension.p6spy.MybatisPlusLogFactory

# 标准p6spy
modulelist=com.p6spy.engine.logging.P6LogFactory

# 带慢SQL检测
modulelist=com.baomidou.mybatisplus.extension.p6spy.MybatisPlusLogFactory,com.p6spy.engine.outage.P6OutageFactory
```

### excludecategories (过滤分类)

可选值：
- `info`: 信息日志
- `debug`: 调试日志
- `statement`: SQL语句
- `commit`: 事务提交
- `rollback`: 事务回滚
- `result`: 查询结果
- `resultset`: 结果集
- `batch`: 批量操作

**推荐配置**（只保留SQL日志）：
```properties
excludecategories=info,debug,result,commit,resultset
```

## 🚀 使用场景

### 1. SQL调试

查看实际执行的SQL语句，包含参数替换：

```java
// 业务代码
userMapper.selectById(1L);

// p6spy输出
Execute SQL：SELECT * FROM sys_user WHERE id = 1
```

### 2. 性能分析

快速定位慢查询：

```log
⚠️  Consume Time：2150 ms 2025-12-11 10:00:00  [SLOW SQL]
 Execute SQL：SELECT * FROM sys_user WHERE status = 1
```

### 3. JOIN优化

查看复杂查询的实际执行SQL：

```log
 Consume Time：45 ms
 Execute SQL：SELECT u.*, r.role_name 
              FROM sys_user u 
              LEFT JOIN sys_user_role ur ON u.id = ur.user_id 
              LEFT JOIN sys_role r ON ur.role_id = r.id 
              WHERE u.id = 1
```

## ⚠️ 注意事项

### 1. 环境限制

| 环境 | 启用p6spy | 配置 |
|------|----------|------|
| **开发** | ✅ 推荐 | `spring.datasource.driver-class-name=com.p6spy.engine.spy.P6SpyDriver` |
| **测试** | 可选 | 根据需要决定 |
| **生产** | ❌ **禁止** | 必须使用 `com.mysql.cj.jdbc.Driver` |

### 2. 性能影响

p6spy会带来轻微的性能开销：

- **SQL执行开销**: 约 1-5ms
- **日志输出开销**: 取决于appender类型
- **内存开销**: 每条SQL约 1-2KB

> 💡 **建议**: 生产环境**必须禁用**，使用专业的APM工具（如SkyWalking）替代。

### 3. 日志量控制

大量SQL会产生大量日志：

```properties
# 方案1: 只在DEBUG模式下启用
logging:
  level:
    p6spy: ${LOG_LEVEL_P6SPY:OFF}  # 通过环境变量控制

# 方案2: 使用excludecategories过滤
excludecategories=info,debug,result,commit,resultset

# 方案3: 设置慢SQL阈值
outagedetectioninterval=5  # 只记录>5秒的SQL
```

## 🔧 故障排查

### 问题1: SQL日志不显示

**症状**: 应用启动正常，但看不到SQL日志

**排查步骤**:

1. 检查驱动配置
   ```yaml
   # ❌ 错误
   driver-class-name: com.mysql.cj.jdbc.Driver
   
   # ✅ 正确
   driver-class-name: com.p6spy.engine.spy.P6SpyDriver
   ```

2. 检查URL前缀
   ```yaml
   # ❌ 错误
   url: jdbc:mysql://localhost:3306/blog_db
   
   # ✅ 正确
   url: jdbc:p6spy:mysql://localhost:3306/blog_db
   ```

3. 检查spy.properties
   ```bash
   # 确认文件存在
   ls -la blog-application/src/main/resources/spy.properties
   
   # 检查excludecategories配置
   grep excludecategories spy.properties
   ```

### 问题2: 启动失败

**症状**: 应用启动报错 `Cannot load driver class: com.p6spy.engine.spy.P6SpyDriver`

**解决**:

```xml
<!-- 确认依赖已添加 -->
<dependency>
    <groupId>p6spy</groupId>
    <artifactId>p6spy</artifactId>
    <version>3.9.1</version>
</dependency>
```

### 问题3: 日志重复

**症状**: 同一条SQL输出多次

**原因**: spy.properties中modulelist配置重复

**解决**:
```properties
# ❌ 错误（重复）
modulelist=com.p6spy.engine.logging.P6LogFactory,com.baomidou.mybatisplus.extension.p6spy.MybatisPlusLogFactory

# ✅ 正确（二选一）
modulelist=com.baomidou.mybatisplus.extension.p6spy.MybatisPlusLogFactory
```

## 📚 最佳实践

### 1. 开发环境配置

```yaml title="application-dev.yaml"
spring:
  datasource:
    driver-class-name: com.p6spy.engine.spy.P6SpyDriver
    url: jdbc:p6spy:mysql://localhost:3306/blog_db

logging:
  level:
    p6spy: INFO
    com.blog: DEBUG
```

```properties title="spy.properties"
# 控制台输出，方便实时查看
appender=com.baomidou.mybatisplus.extension.p6spy.StdoutLogger
outagedetection=true
outagedetectioninterval=2
```

### 2. 生产环境配置

```yaml title="application-prod.yaml"
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver  # 禁用p6spy
    url: jdbc:mysql://localhost:3306/blog_db
```

### 3. 与MyBatis-Plus集成

使用MyBatis-Plus专用格式化器获得更好的输出：

```properties
modulelist=com.baomidou.mybatisplus.extension.p6spy.MybatisPlusLogFactory
logMessageFormat=com.baomidou.mybatisplus.extension.p6spy.P6SpyLogger
```

**优势**:
- ✅ 格式化更清晰
- ✅ 支持多行SQL美化
- ✅ 与MyBatis-Plus功能集成

## 🔗 相关资源

- **p6spy官方文档**: https://p6spy.readthedocs.io/
- **GitHub**: https://github.com/p6spy/p6spy
- **MyBatis-Plus文档**: https://baomidou.com/
- **MyBatis-Plus示例**: https://gitee.com/baomidou/mybatis-plus-samples

---

**文档维护**: 如有任何问题或建议，请提交Issue或PR。

---
sidebar_position: 2
---

# Micrometer 指标监控

Micrometer 是 Spring Boot 的默认指标收集框架，为 JVM 应用提供了供应商中立的应用指标门面。

:::tip 核心概念
Micrometer 类似于 SLF4J，但用于指标而非日志。它提供统一的 API，支持多种监控系统（Prometheus、Graphite、Datadog 等）。
:::

---

## 📊 什么是 Micrometer

Micrometer 是一个**通用指标收集框架**，主要特点：

- 📈 **多维度指标** - 支持 Counter、Gauge、Timer、Distribution Summary
- 🏷️ **标签化** - 使用 Tag 支持多维度数据查询
- 🔌 **多后端支持** - Prometheus、Graphite、InfluxDB、Datadog等
- 🎯 **自动收集** - JVM、HTTP、数据库连接池等开箱即用
- 🔧 **易于扩展** - 简单的 API 自定义业务指标

---

## 🎯 核心概念

### 1. Meter（计量器）

Meter 是 Micrometer 的核心接口，代表一个指标收集点。

#### Counter（计数器）

**用途**: 只增不减的单调递增计数器

**示例**: HTTP 请求总数、订单总数、错误总数

```java
// 使用示例
Counter counter = Counter.builder("orders.created")
    .tag("region", "asia")
    .description("Total orders created")
    .register(meterRegistry);

counter.increment();      // +1
counter.increment(10);    // +10
```

#### Gauge（仪表）

**用途**: 可增可减的瞬时值

**示例**: CPU 使用率、内存使用量、队列大小

```java
// 监控队列大小
List<String> queue = new ArrayList<>();
Gauge.builder("queue.size", queue, List::size)
    .register(meterRegistry);
```

#### Timer（计时器）

**用途**: 测量短时间延迟和事件频率

**示例**: HTTP 请求响应时间、方法执行时间

```java
Timer timer = Timer.builder("http.requests")
    .tag("uri", "/api/users")
    .register(meterRegistry);

timer.record(() -> {
    // 执行操作
    userService.getUsers();
});
```

#### Distribution Summary（分布摘要）

**用途**: 记录事件的分布情况

**示例**: 请求负载大小、响应数据量

```java
DistributionSummary summary = DistributionSummary.builder("request.size")
    .baseUnit("bytes")
    .register(meterRegistry);

summary.record(requestBody.length());
```

---

### 2. MeterRegistry（注册中心）

`MeterRegistry` 是所有 Meter 的容器，负责管理和发布指标。

**注入方式**:
```java
@Service
@RequiredArgsConstructor
public class MyService {
    private final MeterRegistry meterRegistry;  // Spring 自动注入
    
    public void doSomething() {
        Counter counter = Counter.builder("my.counter")
            .register(meterRegistry);
        counter.increment();
    }
}
```

---

### 3. Tags（标签）

Tags 用于为指标添加维度，支持多维度查询。

**示例**:
```java
Counter.builder("http.requests")
    .tag("method", "GET")
    .tag("status", "200")
    .tag("uri", "/api/users")
    .register(meterRegistry);
```

**Prometheus 查询**:
```promql
# 查询特定 URI 的请求数
http_requests_total{uri="/api/users"}

# 查询所有 200 状态的请求
http_requests_total{status="200"}
```

---

## ⚙️ 项目配置

### 1. 启用指标收集

**配置文件**: `blog-application/src/main/resources/application.yaml`

```yaml
management:
  metrics:
    enable:
      jvm: true                      # JVM 指标
      system: true                   # 系统指标
      http.server.requests: true     # HTTP 请求指标
      datasource: true               # 数据源指标
    tags:
      application: ${spring.application.name}  # 全局标签
```

---

### 2. Prometheus 导出配置

```yaml
management:
  prometheus:
    metrics:
      export:
        enabled: true      # 启用 Prometheus 导出
        step: 1m           # 采集间隔（1分钟）
        descriptions: true # 包含指标描述
```

**访问地址**: `http://localhost:8080/actuator/prometheus`

---

## 📈 自动收集的指标

### 1. JVM 指标

| 指标名 | 描述 | 标签 |
|--------|------|------|
| `jvm.memory.used` | 已使用内存 | area(heap/nonheap), id(内存区) |
| `jvm.memory.max` | 最大内存 | area, id |
| `jvm.gc.pause` | GC 暂停时间 | action, cause |
| `jvm.threads.live` | 活跃线程数 | - |
| `jvm.threads.daemon` | 守护线程数 | - |
| `jvm.classes.loaded` | 已加载类数 | - |

**查询示例**:
```bash
# 查看堆内存使用
curl http://localhost:8080/actuator/metrics/jvm.memory.used?tag=area:heap

# 查看 GC 暂停时间
curl http://localhost:8080/actuator/metrics/jvm.gc.pause
```

---

### 2. HTTP 请求指标

| 指标名 | 描述 | 标签 |
|--------|------|------|
| `http.server.requests` | HTTP 请求统计 | method, status, uri, exception |

**示例数据**:
```json
{
  "name": "http.server.requests",
  "measurements": [
    {
      "statistic": "COUNT",
      "value": 42.0
    },
    {
      "statistic": "TOTAL_TIME",
      "value": 0.523
    },
    {
      "statistic": "MAX",
      "value": 0.085
    }
  ],
  "availableTags": [
    {
      "tag": "method",
      "values": ["GET", "POST"]
    },
    {
      "tag": "status",
      "values": ["200", "404", "500"]
    },
    {
      "tag": "uri",
      "values": ["/api/users", "/api/articles"]
    }
  ]
}
```

---

### 3. 数据库连接池指标

| 指标名 | 描述 |
|--------|------|
| `jdbc.connections.active` | 活跃连接数 |
| `jdbc.connections.max` | 最大连接数 |
| `jdbc.connections.min` | 最小连接数 |
| `hikaricp.connections.usage` | HikariCP 连接使用率 |

**查询示例**:
```bash
curl http://localhost:8080/actuator/metrics/jdbc.connections.active
```

---

### 4. 系统指标

| 指标名 | 描述 |
|--------|------|
| `system.cpu.usage` | 系统 CPU 使用率 |
| `process.cpu.usage` | 进程 CPU 使用率 |
| `system.cpu.count` | CPU 核心数 |
| `process.uptime` | 进程运行时间 |

---

## 🔧 自定义指标实践

### 1. 通过 MeterRegistry 注册标签

**项目案例**: Bitiful S3 客户端监控

**文件**: `blog-file-service/src/main/java/com/blog/infrastructure/config/BitifulConfig.java`

```java
@Configuration
@RequiredArgsConstructor
public class BitifulConfig {
    
    @Bean
    public S3Client bitifulS3Client(MeterRegistry meterRegistry) {
        // 为 Bitiful S3 客户端注册全局标签
        meterRegistry.config().commonTags(
            "service", "bitiful",
            "region", bitifulProperties.getRegion(),
            "bucket", bitifulProperties.getBucket()
        );
        
        // 创建 S3 客户端...
        return S3Client.builder()
            .region(Region.of(bitifulProperties.getRegion()))
            .endpointOverride(URI.create(bitifulProperties.getEndpoint()))
            .credentialsProvider(credentialsProvider)
            .build();
    }
}
```

**效果**: 所有 S3 相关的指标都会自动带上这些标签，方便筛选和监控。

---

### 2. 自定义 Counter示例

```java
@Service
@RequiredArgsConstructor
public class ArticleService {
    private final MeterRegistry meterRegistry;
    private final ArticleMapper articleMapper;
    
    public void publishArticle(ArticleDTO article) {
        // 发布文章
        articleMapper.insert(article);
        
        // 记录发布计数
        Counter.builder("articles.published")
            .tag("category", article.getCategory())
            .tag("author", article.getAuthor())
            .description("Total articles published")
            .register(meterRegistry)
            .increment();
    }
}
```

---

### 3. 自定义 Timer 示例

```java
@Service
@RequiredArgsConstructor
public class UserService {
    private final MeterRegistry meterRegistry;
    
    public UserDTO getUser(Long id) {
        Timer timer = Timer.builder("user.query.time")
            .tag("method", "getById")
            .register(meterRegistry);
        
        return timer.record(() -> {
            // 执行查询
            return userMapper.selectById(id);
        });
    }
}
```

---

### 4. 自定义 Gauge 示例

```java
@Component
@RequiredArgsConstructor
public class CacheMetrics {
    private final MeterRegistry meterRegistry;
    private final CacheManager cacheManager;
    
    @PostConstruct
    public void registerCacheMetrics() {
        Cache articleCache = cacheManager.getCache("articles");
        
        Gauge.builder("cache.size", articleCache, cache -> {
            // 假设缓存实现支持 size() 方法
            return cache.getNativeCache().size();
        })
        .tag("cache", "articles")
        .description("Article cache size")
        .register(meterRegistry);
    }
}
```

---

## 📊 Prometheus 集成

### 1. 访问 Prometheus 端点

**URL**: `http://localhost:8080/actuator/prometheus`

**响应格式**:
```prometheus
# HELP jvm_memory_used_bytes The amount of used memory
# TYPE jvm_memory_used_bytes gauge
jvm_memory_used_bytes{application="personal-blog-backend",area="heap",id="G1 Eden Space"} 1.6777216E7
jvm_memory_used_bytes{application="personal-blog-backend",area="heap",id="G1 Survivor Space"} 2097152.0
jvm_memory_used_bytes{application="personal-blog-backend",area="heap",id="G1 Old Gen"} 2.4084048E7

# HELP http_server_requests_seconds  
# TYPE http_server_requests_seconds summary
http_server_requests_seconds_count{application="personal-blog-backend",exception="None",method="GET",status="200",uri="/api/users"} 42.0
http_server_requests_seconds_sum{application="personal-blog-backend",exception="None",method="GET",status="200",uri="/api/users"} 0.523

# HELP jdbc_connections_active Number of active connections
# TYPE jdbc_connections_active gauge
jdbc_connections_active{application="personal-blog-backend",name="HikariPool-1"} 5.0
```

---

### 2. Prometheus 配置

**Prometheus 配置文件** (`prometheus.yml`):
```yaml
scrape_configs:
  - job_name: 'spring-boot-app'
    metrics_path: '/actuator/prometheus'
    scrape_interval: 15s
    static_configs:
      - targets: ['localhost:8080']
        labels:
          application: 'personal-blog-backend'
          environment: 'dev'
```

---

### 3. 常用 PromQL 查询

```promql
# JVM 堆内存使用率
(jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}) * 100

# HTTP 请求 QPS
rate(http_server_requests_seconds_count[1m])

# HTTP 请求平均响应时间
rate(http_server_requests_seconds_sum[1m]) / rate(http_server_requests_seconds_count[1m])

# 数据库连接池使用率
(jdbc_connections_active / jdbc_connections_max) * 100

# 进程 CPU 使用率
process_cpu_usage * 100
```

---

## 📋 常用指标查询

### 1. 查看所有可用指标

```bash
curl http://localhost:8080/actuator/metrics
```

### 2. 查询 JVM 内存

```bash
# 已使用内存
curl http://localhost:8080/actuator/metrics/jvm.memory.used

# 堆内存
curl http://localhost:8080/actuator/metrics/jvm.memory.used?tag=area:heap

# Eden 区
curl "http://localhost:8080/actuator/metrics/jvm.memory.used?tag=area:heap&tag=id:G1%20Eden%20Space"
```

### 3. 查询 HTTP 请求

```bash
# 所有请求统计
curl http://localhost:8080/actuator/metrics/http.server.requests

# 特定 URI
curl "http://localhost:8080/actuator/metrics/http.server.requests?tag=uri:/api/users"

# 特定状态码
curl "http://localhost:8080/actuator/metrics/http.server.requests?tag=status:200"
```

### 4. 查询数据库连接池

```bash
curl http://localhost:8080/actuator/metrics/jdbc.connections.active
curl http://localhost:8080/actuator/metrics/jdbc.connections.max
curl http://localhost:8080/actuator/metrics/hikaricp.connections.usage
```

---

## 🎯 最佳实践

### 1. 合理使用标签

✅ **好的标签**:
```java
Counter.builder("orders.created")
    .tag("region", "asia")        // 合理：区域
    .tag("channel", "web")        // 合理：渠道
    .register(meterRegistry);
```

❌ **不好的标签**:
```java
Counter.builder("orders.created")
    .tag("user_id", userId)       // 避免：高基数
    .tag("order_id", orderId)     // 避免：高基数
    .register(meterRegistry);
```

:::warning 高基数问题
避免使用用户 ID、订单 ID 等高基数值作为标签，这会导致指标数量爆炸，消耗大量内存。
:::

---

### 2. 统一命名规范

**推荐命名**:
- `http.requests` - HTTP 请求
- `db.queries` - 数据库查询
- `cache.hits` - 缓存命中
- `queue.size` - 队列大小

**避免**:
- `HttpRequests` - 大小写混合
- `http_requests` - 下划线（应使用点号）

---

### 3. 添加有意义的描述

```java
Counter.builder("articles.published")
    .tag("category", category)
    .description("Total number of published articles by category")  // ✅ 有描述
    .baseUnit("articles")
    .register(meterRegistry);
```

---

### 4. 使用 Timer 而非手动计时

❌ **不推荐**:
```java
long start = System.currentTimeMillis();
doSomething();
long duration = System.currentTimeMillis() - start;
```

✅ **推荐**:
```java
Timer timer = Timer.builder("operation.duration").register(meterRegistry);
timer.record(() -> doSomething());
```

---

## 🐛 常见问题

### Q1: 指标不显示

**原因**: 
1. 指标收集未启用
2. 指标还未产生数据

**解决**:
```yaml
management:
  metrics:
    enable:
      jvm: true
      http.server.requests: true
```

### Q2: Prometheus 端点返回 404

**原因**: Prometheus 导出未启用

**解决**:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: prometheus  # 暴露端点
```

### Q3: 如何删除指标？

Micrometer 不支持删除指标。建议在创建时使用合理的过期策略。

---

## 📚 延伸阅读

- [Actuator 使用指南](./actuator.md)
- [Spring Boot Admin 管理界面](./spring-boot-admin.md)
- [Micrometer 官方文档](https://micrometer.io/docs)
- [Prometheus 官方文档](https://prometheus.io/docs/)

---

**文档更新日期**: 2025-12-11  
**Micrometer 版本**: 1.13.x (Spring Boot 3.5.7 内置)

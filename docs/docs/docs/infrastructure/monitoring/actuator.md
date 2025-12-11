---
sidebar_position: 1
---

# Spring Boot Actuator 使用指南

Spring Boot Actuator 提供了生产级别的监控和管理功能，通过一系列 HTTP 端点暴露应用的运行时信息。

:::tip 核心价值
Actuator 让你无需额外开发即可获得应用健康检查、指标监控、环境信息等生产必备功能。
:::

---

## 📚 什么是 Spring Boot Actuator

Actuator 是 Spring Boot 提供的**监控和管理模块**，主要功能包括：

- 🏥 **健康检查** - 检测应用和依赖服务的健康状态
- 📊 **指标收集** - 收集 JVM、HTTP、数据库等运行指标
- 📝 **日志管理** - 动态调整日志级别
- 🔧 **环境信息** - 查看配置属性和环境变量
- 🌐 **请求映射** - 查看所有 Controller 端点

---

## 🎯 为什么需要 Actuator

### 传统监控的痛点

❌ **没有 Actuator 时**:
- 手动实现健康检查接口
- 通过日志排查性能问题
- 无法实时查看 JVM 内存使用
- 配置错误难以发现

✅ **使用 Actuator 后**:
- 开箱即用的健康检查
- 实时查看各类指标
- 动态调整日志级别
- 与监控系统无缝集成（Prometheus、Spring Boot Admin）

---

## 🔧 项目配置详解

### 1. 依赖配置

Actuator 已包含在 `spring-boot-starter-parent` 中，无需额外添加依赖。

**验证依赖**（可选）:
```xml
<!-- blog-application/pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

### 2. 端点配置

**配置文件**: `blog-application/src/main/resources/application.yaml`

```yaml
management:
  endpoints:
    web:
      base-path: /actuator                    # 端点基础路径
      exposure:
        include:                              # 暴露的端点列表
          - health        # 健康检查
          - info          # 应用信息
          - logfile       # 日志文件
          - metrics       # 指标数据
          - env           # 环境变量
          - loggers       # 日志配置
          - beans         # Spring Bean 列表
          - mappings      # 请求映射列表
          - threaddump    # 线程转储
          - auditevents   # 审计事件
          - prometheus    # Prometheus 格式指标
```

:::warning 安全提示
生产环境应该谨慎暴露端点，建议：
- 仅暴露必要的端点（health、info、metrics）
- 使用独立端口（`management.server.port`）
- 配置访问控制（Spring Security）
:::

### 3. 安全配置

在 `SecurityConfig` 中将 Actuator 端点加入白名单：

```yaml
# application.yaml
app:
  security:
    permit-all-urls:
      - "/actuator/**"    # 允许访问所有 Actuator 端点
```

:::info 说明
本项目为了方便开发和演示，将 Actuator 端点设为公开访问。生产环境建议配置认证。
:::

---

## 📊 常用端点详解

### 1. Health 端点 - 健康检查

**访问地址**: `GET http://localhost:8080/actuator/health`

#### 基础响应

```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "MySQL",
        "validationQuery": "isValid()"
      }
    },
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": 250685575168,
        "free": 100000000000,
        "threshold": 10485760,
        "path": "/Users/liusx/..."
      }
    },
    "ping": {
      "status": "UP"
    },
    "redis": {
      "status": "UP",
      "details": {
        "version": "7.2.0"
      }
    }
  }
}
```

#### 配置详细信息

```yaml
management:
  endpoint:
    health:
      show-details: always    # 始终显示详细信息
      # 可选值: never, when-authorized, always
```

**状态说明**:
- `UP` - 健康
- `DOWN` - 不健康
- `OUT_OF_SERVICE` - 暂停服务
- `UNKNOWN` - 未知状态

---

### 2. Info 端点 - 应用信息

**访问地址**: `GET http://localhost:8080/actuator/info`

#### 响应示例

```json
{
  "app": {
    "name": "personal-blog-backend",
    "description": "Personal Blog Backend",
    "version": "1.0-SNAPSHOT",
    "encoding": "UTF-8",
    "java": {
      "version": "21"
    }
  },
  "git": {
    "branch": "main",
    "commit": {
      "id": "abc1234",
      "time": "2025-12-11T14:20:00Z"
    }
  },
  "build": {
    "artifact": "blog-application",
    "name": "blog-application",
    "time": "2025-12-11T14:30:00.000Z",
    "version": "1.0-SNAPSHOT",
    "group": "com.blog"
  }
}
```

#### 配置说明

**1. 启用信息收集**

```yaml
management:
  info:
    env:
      enabled: true    # 启用环境信息
    git:
      enabled: true    # 启用 Git 信息
      mode: full       # 显示详细 Git 信息
    build:
      enabled: true    # 启用构建信息
```

**2. Maven 资源过滤**

在 `application.yaml` 中使用 Maven 变量：

```yaml
info:
  app:
    name: '@project.name@'
    description: '@project.description@'
    version: '@project.version@'
    encoding: '@project.build.sourceEncoding@'
    java:
      version: '@java.version@'
```

**3. 启用 Git 信息插件**

在 `pom.xml` 中添加：

```xml
<build>
  <plugins>
    <plugin>
      <groupId>io.github.git-commit-id</groupId>
      <artifactId>git-commit-id-maven-plugin</artifactId>
      <executions>
        <execution>
          <goals>
            <goal>revision</goal>
          </goals>
        </execution>
      </executions>
      <configuration>
        <generateGitPropertiesFile>true</generateGitPropertiesFile>
      </configuration>
    </plugin>
  </plugins>
</build>
```

---

### 3. Metrics 端点 - 指标数据

**访问地址**: `GET http://localhost:8080/actuator/metrics`

#### 可用指标列表

```json
{
  "names": [
    "jvm.memory.used",
    "jvm.memory.max",
    "jvm.threads.live",
    "jvm.gc.pause",
    "http.server.requests",
    "jdbc.connections.active",
    "jdbc.connections.max",
    "system.cpu.usage",
    "process.uptime"
  ]
}
```

#### 查询特定指标

**请求**: `GET http://localhost:8080/actuator/metrics/jvm.memory.used`

**响应**:
```json
{
  "name": "jvm.memory.used",
  "description": "The amount of used memory",
  "baseUnit": "bytes",
  "measurements": [
    {
      "statistic": "VALUE",
      "value": 268435456
    }
  ],
  "availableTags": [
    {
      "tag": "area",
      "values": ["heap", "nonheap"]
    },
    {
      "tag": "id",
      "values": ["G1 Old Gen", "G1 Survivor Space", "G1 Eden Space"]
    }
  ]
}
```

#### 常用指标查询

```bash
# JVM 内存使用
curl http://localhost:8080/actuator/metrics/jvm.memory.used

# HTTP 请求统计
curl http://localhost:8080/actuator/metrics/http.server.requests

# 数据库连接池
curl http://localhost:8080/actuator/metrics/jdbc.connections.active

# CPU 使用率
curl http://localhost:8080/actuator/metrics/system.cpu.usage
```

---

### 4. Env 端点 - 环境变量

**访问地址**: `GET http://localhost:8080/actuator/env`

查看所有配置属性和环境变量（**包含敏感信息，需谨慎暴露**）。

**响应示例**（部分）:
```json
{
  "activeProfiles": ["dev"],
  "propertySources": [
    {
      "name": "systemProperties",
      "properties": {
        "java.version": {
          "value": "21.0.1"
        }
      }
    },
    {
      "name": "applicationConfig: [classpath:/application.yaml]",
      "properties": {
        "spring.application.name": {
          "value": "personal-blog-backend"
        },
        "server.port": {
          "value": 8080
        }
      }
    }
  ]
}
```

---

### 5. Loggers 端点 - 日志管理

**访问地址**: `GET http://localhost:8080/actuator/loggers`

#### 查看所有 Logger

```json
{
  "levels": ["OFF", "ERROR", "WARN", "INFO", "DEBUG", "TRACE"],
  "loggers": {
    "ROOT": {
      "configuredLevel": "INFO",
      "effectiveLevel": "INFO"
    },
    "com.blog": {
      "configuredLevel": "INFO",
      "effectiveLevel": "INFO"
    }
  }
}
```

#### 动态修改日志级别

**请求**: `POST http://localhost:8080/actuator/loggers/com.blog`

**请求体**:
```json
{
  "configuredLevel": "DEBUG"
}
```

**结果**: `com.blog` 包的日志级别立即从 `INFO` 改为 `DEBUG`，无需重启应用。

---

### 6. Prometheus 端点 - 指标导出

**访问地址**: `GET http://localhost:8080/actuator/prometheus`

#### 配置

```yaml
management:
  prometheus:
    metrics:
      export:
        enabled: true      # 启用 Prometheus 导出
        step: 1m           # 采集间隔
        descriptions: true # 包含指标描述
```

#### 响应格式

```prometheus
# HELP jvm_memory_used_bytes The amount of used memory
# TYPE jvm_memory_used_bytes gauge
jvm_memory_used_bytes{application="personal-blog-backend",area="heap",id="G1 Eden Space"} 1.6777216E7

# HELP http_server_requests_seconds  
# TYPE http_server_requests_seconds summary
http_server_requests_seconds_count{application="personal-blog-backend",method="GET",status="200",uri="/api/users"} 42.0
http_server_requests_seconds_sum{application="personal-blog-backend",method="GET",status="200",uri="/api/users"} 0.523
```

**用途**: 供 Prometheus 采集，用于时序数据库存储和 Grafana 可视化。

---

## 🏥 Health 端点高级配置

### 1. K8s 探针支持

```yaml
management:
  endpoint:
    health:
      probes:
        enabled: true    # 启用 Kubernetes 探针
```

**Liveness 探针**: `GET /actuator/health/liveness`
```json
{
  "status": "UP"
}
```

**Readiness 探针**: `GET /actuator/health/readiness`
```json
{
  "status": "UP"
}
```

**Kubernetes 配置示例**:
```yaml
apiVersion: v1
kind: Pod
spec:
  containers:
  - name: blog-app
    livenessProbe:
      httpGet:
        path: /actuator/health/liveness
        port: 8080
      initialDelaySeconds: 30
      periodSeconds: 10
    readinessProbe:
      httpGet:
        path: /actuator/health/readiness
        port: 8080
      initialDelaySeconds: 10
      periodSeconds: 5
```

---

## 🔗 与 Swagger 集成

项目配置了 Actuator 端点在 Swagger UI 中显示：

```yaml
springdoc:
  show-actuator: true    # 在 Swagger 中显示 Actuator 端点
```

**访问**: `http://localhost:8080/swagger-ui.html`

你可以在 Swagger UI 中直接测试 Actuator 端点，无需使用 curl 或 Postman。

---

## 📝 访问示例

### 使用 curl

```bash
# 健康检查
curl http://localhost:8080/actuator/health

# 应用信息
curl http://localhost:8080/actuator/info

# 查看所有指标
curl http://localhost:8080/actuator/metrics

# 查看 JVM 内存
curl http://localhost:8080/actuator/metrics/jvm.memory.used

# 修改日志级别
curl -X POST http://localhost:8080/actuator/loggers/com.blog \
  -H "Content-Type: application/json" \
  -d '{"configuredLevel":"DEBUG"}'
```

### 使用浏览器

直接访问：
- `http://localhost:8080/actuator`
- `http://localhost:8080/actuator/health`
- `http://localhost:8080/actuator/info`
- `http://localhost:8080/actuator/metrics`

---

## 🐛 常见问题

### Q1: 端点返回 404

**原因**: 端点未暴露或路径错误

**解决**:
1. 检查 `management.endpoints.web.exposure.include` 配置
2. 确认端点名称正确（`health` 而非 `Health`）
3. 验证基础路径（默认 `/actuator`）

---

### Q2: Health 端点不显示详细信息

**原因**: `show-details` 配置不正确

**解决**:
```yaml
management:
  endpoint:
    health:
      show-details: always  # 确保设置为 always
```

---

### Q3: Info 端点返回空对象 {}

**原因**: 
1. Maven 资源过滤未启用
2. Git 插件未配置

**解决**:
1. 在 `pom.xml` 启用资源过滤：
```xml
<resources>
  <resource>
    <directory>src/main/resources</directory>
    <filtering>true</filtering>
  </resource>
</resources>
```

2. 添加 `git-commit-id-maven-plugin`

---

### Q4: 如何禁用特定健康指标？

**示例**: 禁用 Redis 健康检查

```yaml
management:
  health:
    redis:
      enabled: false
```

---

## 🎯 最佳实践

### 1. 生产环境配置

```yaml
# application-prod.yaml
management:
  server:
    port: 8081        # 使用独立端口
  endpoints:
    web:
      exposure:
        include: health,info,metrics  # 仅暴露必要端点
  endpoint:
    health:
      show-details: when-authorized   # 需要认证才显示详情
```

### 2. 监控集成

- **Prometheus**: 使用 `/actuator/prometheus` 端点
- **Spring Boot Admin**: 自动发现所有端点
- **ELK**: 通过 `/actuator/logfile` 收集日志

### 3. 安全加固

```java
@Configuration
public class ActuatorSecurityConfig {
    @Bean
    public SecurityFilterChain actuatorSecurityFilterChain(HttpSecurity http) {
        http.securityMatcher("/actuator/**")
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .anyRequest().hasRole("ADMIN")
            );
        return http.build();
    }
}
```

---

## 📚 延伸阅读

- [Micrometer 指标监控](./micrometer.md)
- [Spring Boot Admin 管理界面](./spring-boot-admin.md)
- [Spring Boot Actuator 官方文档](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)

---

**文档更新日期**: 2025-12-11  
**Actuator 版本**: Spring Boot 3.5.7

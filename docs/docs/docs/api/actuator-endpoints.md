---
sidebar_position: 5
---

# Actuator 端点 API

Spring Boot Actuator 提供了一系列监控和管理端点，用于查看应用运行状态、指标数据和健康信息。

:::tip 访问地址
所有 Actuator 端点的基础路径为 `/actuator`。例如：`http://localhost:8080/actuator/health`
:::

---

## 🔓 认证说明

本项目的 Actuator 端点配置为**公开访问**（已加入白名单），无需 JWT Token。

```yaml
# 白名单配置
app:
  security:
    permit-all-urls:
      - "/actuator/**"
```

:::warning 生产环境建议
生产环境应该限制 Actuator 端点访问，建议：
- 使用独立端口（`management.server.port`）
- 配置 IP 白名单
- 启用 Spring Security 认证
:::

---

## 📊 可用端点列表

| 端点 | 方法 | 功能 | 响应格式 |
|------|------|------|---------|
| `/actuator` | GET | 端点列表（发现端点） | JSON |
| `/actuator/health` | GET | 健康检查 | JSON |
| `/actuator/info` | GET | 应用信息 | JSON |
| `/actuator/metrics` | GET | 指标列表 | JSON |
| `/actuator/metrics/{name}` | GET | 特定指标 | JSON |
| `/actuator/env` | GET | 环境变量 | JSON |
| `/actuator/loggers` | GET | 日志配置 | JSON |
| `/actuator/loggers/{name}` | POST | 修改日志级别 | - |
| `/actuator/logfile` | GET | 日志文件 | Text |
| `/actuator/beans` | GET | Spring Bean 列表 | JSON |
| `/actuator/mappings` | GET | 请求映射列表 | JSON |
| `/actuator/threaddump` | GET | 线程转储 | JSON |
| `/actuator/prometheus` | GET | Prometheus 格式指标 | Text |

---

## 📍 端点详解

### 1. Health - 健康检查

**请求**:
```http
GET /actuator/health
```

**响应示例**:
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
        "exists": true
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

**状态码**:
- `UP` - 健康
- `DOWN` - 不健康
- `OUT_OF_SERVICE` - 暂停服务
- `UNKNOWN` - 未知

**用途**:
- 健康检查探针（Kubernetes Liveness/Readiness）
- 负载均衡器健康检查
- 监控系统集成

---

### 2. Info - 应用信息

**请求**:
```http
GET /actuator/info
```

**响应示例**:
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

**用途**:
- 查看应用版本信息
- 查看 Git 构建信息
- 部署验证

---

### 3. Metrics - 指标查询

#### 3.1 获取指标列表

**请求**:
```http
GET /actuator/metrics
```

**响应示例**:
```json
{
  "names": [
    "jvm.memory.used",
    "jvm.memory.max",
    "jvm.threads.live",
    "jvm.gc.pause",
    "http.server.requests",
    "jdbc.connections.active",
    "system.cpu.usage",
    "process.uptime"
  ]
}
```

#### 3.2 查询特定指标

**请求**:
```http
GET /actuator/metrics/jvm.memory.used
```

**响应示例**:
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
      "values": ["G1 Old Gen", "G1 Survivor Space"]
    }
  ]
}
```

#### 3.3 按标签过滤

**请求**:
```http
GET /actuator/metrics/jvm.memory.used?tag=area:heap
```

**常用指标**:

| 指标名 | 描述 |
|--------|------|
| `jvm.memory.used` | JVM 内存使用 |
| `jvm.threads.live` | 活跃线程数 |
| `jvm.gc.pause` | GC 暂停时间 |
| `http.server.requests` | HTTP 请求统计 |
| `jdbc.connections.active` | 数据库活跃连接 |
| `system.cpu.usage` | 系统 CPU 使用率 |
| `process.uptime` | 进程运行时间（秒） |

---

### 4. Env - 环境变量

**请求**:
```http
GET /actuator/env
```

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

**用途**:
- 查看配置属性
- 排查配置问题
- 验证环境变量

:::warning 安全提示
敏感信息（如密码）会被自动脱敏显示为 `******`
:::

---

### 5. Loggers - 日志管理

#### 5.1 查看日志配置

**请求**:
```http
GET /actuator/loggers
```

**响应示例**（部分）:
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

#### 5.2 动态修改日志级别

**请求**:
```http
POST /actuator/loggers/com.blog
Content-Type: application/json

{
  "configuredLevel": "DEBUG"
}
```

**响应**: `204 No Content`

**用途**:
- 线上问题排查
- 临时开启 DEBUG 日志
- 无需重启应用

---

### 6. Prometheus - 指标导出

**请求**:
```http
GET /actuator/prometheus
```

**响应格式**: Prometheus Text Format

**响应示例**:
```prometheus
# HELP jvm_memory_used_bytes The amount of used memory
# TYPE jvm_memory_used_bytes gauge
jvm_memory_used_bytes{application="personal-blog-backend",area="heap"} 1.6777216E7

# HELP http_server_requests_seconds  
# TYPE http_server_requests_seconds summary
http_server_requests_seconds_count{application="personal-blog-backend",method="GET",status="200",uri="/api/users"} 42.0
http_server_requests_seconds_sum{application="personal-blog-backend",method="GET",status="200",uri="/api/users"} 0.523
```

**用途**:
- Prometheus 监控系统采集
- Grafana 可视化
- 时序数据存储

---

## 🔨 使用示例

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

# 查看 Prometheus 指标
curl http://localhost:8080/actuator/prometheus
```

### 使用 HTTPie

```bash
# 健康检查
http :8080/actuator/health

# 修改日志级别
http POST :8080/actuator/loggers/com.blog configuredLevel=DEBUG
```

### 使用 Swagger UI

访问 `http://localhost:8080/swagger-ui.html` 可以直接在浏览器中测试 Actuator 端点（已配置 `showActuator: true`）。

---

## 🔗 集成示例

### Kubernetes 健康探针

```yaml
apiVersion: v1
kind: Pod
spec:
  containers:
  - name: blog-app
    image: personal-blog-backend:latest
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

### Prometheus 采集配置

```yaml
scrape_configs:
  - job_name: 'spring-boot-app'
    metrics_path: '/actuator/prometheus'
    scrape_interval: 15s
    static_configs:
      - targets: ['localhost:8080']
```

---

## 📊 监控看板

通过 Spring Boot Admin 可以获得可视化监控界面：

**访问地址**: `http://localhost:9000`

**功能**:
- 🏥 健康状态实时监控
- 📊 JVM 指标图表
- 📝 在线日志查看
- 🧵 线程和堆栈分析

[查看 Spring Boot Admin 文档 →](../infrastructure/monitoring/spring-boot-admin)

---

## 🔗 相关文档

- [Actuator 使用指南](../infrastructure/monitoring/actuator) - 详细配置和最佳实践
- [Micrometer 指标监控](../infrastructure/monitoring/micrometer) - 自定义指标
- [Spring Boot Admin](../infrastructure/monitoring/spring-boot-admin) - 可视化监控界面

---

**最后更新**: 2025-12-11  
**Actuator 版本**: Spring Boot 3.5.7

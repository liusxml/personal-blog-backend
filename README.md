# personal-blog-backend

个人博客后端服务，基于 Spring Boot 3 + Java 21 的多模块 Maven 项目，提供博客核心 REST API、JWT 认证鉴权、基于 LangChain4j 的 AI 问答与 Ops 运维 Agent、对象存储集成及完整的可观测性支持。

---

## 技术栈

| 类别 | 技术 | 版本 |
|:---|:---|:---|
| 核心框架 | Spring Boot | 3.5.12 |
| 语言 | Java | 21 |
| 构建工具 | Maven | 3.9+ |
| 持久层 | MyBatis-Plus | 3.5.14 |
| 数据库 | MySQL | 9.x |
| 缓存 | Redis（Lettuce）+ Caffeine（本地） | Spring Boot managed |
| 认证 | Spring Security + JJWT | 0.13.0 |
| AI 框架 | LangChain4j | 1.12.2 |
| AI 模型 | 通义千问 DashScope（qwen-plus）| — |
| 向量数据库 | Qdrant Cloud（gRPC）| — |
| SSH 运维 | Apache MINA SSHD | 2.17.1 |
| 对象存储 | Bitiful OSS（AWS S3 兼容 SDK）| AWS SDK 2.38 |
| API 文档 | SpringDoc OpenAPI（Swagger UI）| 2.8.16 |
| 监控 | Spring Boot Actuator + Micrometer + Prometheus | — |
| 监控 UI | Spring Boot Admin | 3.5.5 |
| 链路追踪 | OpenTelemetry Java Agent | 2.26.1 |
| 对象映射 | MapStruct | 1.6.3 |
| 日志 | Logback + Logstash Encoder（JSON）| 9.0 |
| Markdown 解析 | flexmark-all | 0.64.8 |
| 架构测试 | ArchUnit + PlantUML | 1.4.1 |
| 代码简化 | Lombok | 1.18.42 |
| SQL 监控 | P6Spy（仅 dev）| 3.9.1 |
| CI/CD 追踪 | git-commit-id-maven-plugin | 9.0.2 |

---

## 项目结构（多模块 Maven）

```
personal-blog-backend/
├── pom.xml                                      # 根 POM（统一版本管理 + BOM 依赖）
│
├── blog-common/                                 # 公共基础模块（被所有模块依赖）
│   └── src/main/java/com/blog/common/
│       ├── base/                                # BaseEntity（雪花 ID + 自动填充时间字段）
│       ├── config/                              # 公共配置（Jackson、Validation 等）
│       ├── constants/                           # 全局常量（缓存键、业务常量）
│       ├── enums/                               # 业务枚举（文章状态、文件类型等）
│       ├── exception/                           # 全局异常体系（BusinessException + GlobalExceptionHandler）
│       ├── model/                               # 统一响应（Result<T>、PageResult<T>）
│       ├── security/                            # 安全工具（JwtUtils、SecurityUtils）
│       └── utils/                               # 通用工具类
│
├── blog-modules/                                # 业务模块聚合
│   ├── blog-module-system/                      # 系统模块（用户 / 角色 / 认证）
│   │   ├── blog-system-api/                     # API 接口（VO、DTO、接口定义）
│   │   └── blog-system-service/                 # 服务实现
│   │       └── src/.../com/blog/system/
│   │           ├── controller/                  # AuthController、UserController、RoleController
│   │           ├── domain/entity/               # User、Role
│   │           ├── service/                     # 业务逻辑（含密码加密、JWT 签发）
│   │           └── infrastructure/mapper/       # MyBatis-Plus Mapper
│   │
│   ├── blog-module-article/                     # 文章模块（文章 / 分类 / 标签 / 壁纸）
│   │   ├── blog-article-api/                    # API 接口
│   │   └── blog-article-service/
│   │       └── src/.../com/blog/article/
│   │           ├── controller/                  # ArticleController（公开）、ArticleAdminController（管理员）
│   │           │                                # CategoryController、TagController、WallpaperAdminController
│   │           ├── domain/
│   │           │   ├── entity/                  # Article、Category、Tag
│   │           │   ├── state/                   # 文章状态机（DRAFT/PUBLISHED/ARCHIVED）
│   │           │   └── event/                   # 文章发布领域事件
│   │           ├── service/                     # 业务逻辑（含 Markdown→HTML 转换）
│   │           ├── converter/                   # MapStruct Entity↔VO/DTO 转换器
│   │           ├── infrastructure/              # Mapper、Redis 缓存、OSS 集成
│   │           └── metrics/                     # Micrometer 自定义业务指标
│   │
│   ├── blog-module-comment/                     # 评论模块（评论树 / 审核 / 举报）
│   │   ├── blog-comment-api/
│   │   └── blog-comment-service/
│   │       └── src/.../com/blog/comment/
│   │           ├── controller/                  # CommentController、CommentAdminController、CommentAuditController
│   │           ├── domain/                      # Comment 实体 + 举报记录
│   │           ├── service/                     # 评论树构建、审核流
│   │           ├── infrastructure/mapper/
│   │           └── metrics/                     # 评论相关指标
│   │
│   ├── blog-module-file/                        # 文件模块（预签名上传 / OSS 管理）
│   │   ├── blog-file-api/
│   │   └── blog-file-service/
│   │       └── src/.../com/blog/file/
│   │           ├── controller/                  # FileController（预签名 + 确认 + 列表 + 删除）
│   │           ├── domain/entity/               # FileRecord
│   │           ├── service/                     # 预签名生成、MD5 去重、OSS 删除
│   │           ├── infrastructure/              # OSS 适配器（Bitiful S3 兼容）
│   │           └── validator/                   # 文件类型 / 大小校验
│   │
│   └── blog-module-ai/                          # AI 模块（RAG 问答 + Ops 运维 Agent）
│       ├── blog-ai-api/
│       └── blog-ai-service/
│           └── src/.../com/blog/ai/
│               ├── controller/                  # AiController（RAG 问答）、OpsAgentController（SSE 流式）
│               │                                # OpsWebhookController（GitHub Webhook）
│               ├── application/
│               │   ├── agent/                   # OpsAdminAgent（LangChain4j AiService 接口）
│               │   └── tools/                   # RemoteDevOpsTools（@Tool 工具函数集）
│               ├── service/                     # RAG 检索、CI 事件查询
│               ├── domain/                      # CI Event 领域对象
│               ├── infrastructure/
│               │   └── ssh/                     # RemoteSshExecutor、SseEmitterContext、OpsCommandWhitelist
│               └── config/                      # OpsProperties（SSH 配置绑定）
│
├── blog-application/                            # 启动模块（Spring Boot 入口）
│   └── src/main/java/com/blog/
│       ├── BlogApplication.java                 # @SpringBootApplication 入口
│       ├── config/                              # 全局配置（Security、CORS、Redis、MyBatis-Plus 等）
│       ├── handler/                             # 全局异常处理、响应拦截
│       └── security/                            # JwtAuthenticationFilter
│   └── src/main/resources/
│       ├── application.yaml                     # 主配置（激活 dev profile）
│       ├── application-dev.yaml                 # 开发环境（本地地址）
│       ├── application-prod.yaml                # 生产环境（Docker 服务名）
│       └── logback-spring.xml                   # 日志配置（控制台 + JSON 文件 + ELK 兼容）
│
├── blog-admin-server/                           # Spring Boot Admin 独立监控服务（端口 9000）
├── Dockerfile                                   # 主服务 4 阶段构建（builder → extractor → runtime）
├── Dockerfile.admin-server                      # Admin Server 单独 Dockerfile
└── .github/workflows/
    ├── docker-publish.yml                       # CI/CD：main 分支推送后构建并发布主服务镜像
    ├── docker-publish-admin-server.yml          # CI/CD：Admin Server 镜像发布
    ├── codeql.yml                               # CodeQL 代码安全分析
    └── qodana_code_quality.yml                  # Qodana 代码质量检查
```

---

## 模块功能详解

### 系统模块（blog-module-system）

| 功能 | 说明 |
|:---|:---|
| 用户注册 / 登录 | BCrypt 密码加密，登录成功返回 JWT Token |
| 角色权限 | `ROLE_ADMIN` / `ROLE_USER` 两级权限，基于 Spring Security `@PreAuthorize` |
| JWT 认证 | 无状态 Token，JJWT 签发，过期时间可配（默认 2 小时），每次请求由 `JwtAuthenticationFilter` 验证 |
| 当前用户 | `GET /api/v1/users/me` 返回当前登录用户信息 |

### 文章模块（blog-module-article）

| 功能 | 说明 |
|:---|:---|
| 文章 CRUD | 创建、编辑、删除，Markdown 内容服务端转 HTML（flexmark） |
| 生命周期 | 状态机驱动：DRAFT → PUBLISHED → ARCHIVED，对应 `/publish` `/archive` 接口 |
| 向量化 | 发布时自动 Embedding（text-embedding-v4）并写入 Qdrant，支持手动 `/rebuild-embeddings` |
| 多维筛选 | 按分类 / 标签 / 状态 / 关键词分页查询（MyBatis-Plus 动态 SQL） |
| 分类管理 | 树形分类（无限级），支持按 slug 查询 |
| 标签管理 | 标签合并（迁移关联文章），热门标签排序 |
| 文章缓存 | 文章详情 Redis 缓存，发布 / 编辑后自动失效 |
| 封面壁纸 | 从必应壁纸 API 拉取近期图片，供前端选择封面 |
| 相关推荐 | `/articles/{id}/related` 基于标签 / 分类算法返回相关文章 |

### 评论模块（blog-module-comment）

| 功能 | 说明 |
|:---|:---|
| 评论树 | `/comments/tree` 一次性返回嵌套评论树（CommentTreeVO） |
| 匿名发表 | 公开接口，无需登录即可发表评论 |
| 评论回复 | `POST /comments/reply`，支持 @回复 指定用户 |
| 点赞 | `POST /comments/{id}/like`，游客也可点赞 |
| 审核流 | 管理员通过 / 拒绝评论（附原因），强制删除接口 |
| 举报处理 | 用户举报 → 管理员列表审查 → 通过或拒绝 |

### 文件模块（blog-module-file）

预签名上传流程（前端直传 OSS，不占用后端带宽）：

```
前端 POST /files/presigned  →  后端生成预签名 PUT URL
前端 PUT {uploadUrl}        →  直接上传至 Bitiful OSS
前端 PATCH /files/{id}/confirm  →  后端标记上传完成
```

| 功能 | 说明 |
|:---|:---|
| MD5 去重 | 服务端检测同 MD5 文件，直接返回已有记录（秒传） |
| 文件分类 | IMAGE / VIDEO / DOCUMENT / OTHER 枚举分类 |
| 访问 URL | `GET /files/{id}/access-url` 生成带签名的临时访问地址 |
| 文件删除 | 同步删除 OSS 对象与数据库记录 |

### AI 模块（blog-module-ai）

#### RAG 智能问答（AiController）

- 接收用户问题 → DashScope 向量化问题（text-embedding-v4）→ Qdrant 向量检索相关文章 → 通义千问生成回答
- 返回答案文本 + 参考文章列表（来源）

#### Ops 运维 Agent（OpsAgentController）

基于 LangChain4j Function Calling + Apache MINA SSHD 实现的 AI 运维助手：

| 工具方法（@Tool）| 功能 |
|:---|:---|
| `updateService` | 拉取最新镜像并重启（仅限 frontend / admin） |
| `restartService` | 原地重启服务（不更新镜像） |
| `fetchLogs` | 查看指定服务最近 100 行日志 |
| `checkServiceStatus` | 检查所有 Compose 服务运行状态 |
| `updateBackendSelf` | 更新 backend 自身（nohup 异步，防自杀悖论） |

**安全保障（双保险）**：
1. `@Tool` JavaDoc 明确声明合法参数值，约束 LLM 输出范围
2. `OpsCommandWhitelist` 运行时白名单二次校验，拦截非法服务名

**SSE 流式协议**（`text/event-stream`）：

| 事件类型 | 说明 |
|:---|:---|
| `message` | AI 逐 token 流式回复 |
| `ops_log` | SSH 终端实时输出（由 `SseEmitterContext` 推送） |
| `tool_call` | 工具调用完成通知 |
| `done` | 整轮对话结束信号 |
| `error` | 异常事件 |

#### GitHub Webhook（OpsWebhookController）

- 接收 GitHub Actions CI 事件推送
- HMAC-SHA256 签名验证（`OPS_WEBHOOK_SECRET`），本地留空自动跳过
- 存储 CI 运行记录（状态、结论），供 Ops Copilot 查询

---

## REST API 总览

| 模块 | 路径前缀 | 鉴权 | 说明 |
|:---|:---|:---|:---|
| 认证 | `/api/v1/auth` | 公开 | 注册、登录 |
| 用户 | `/api/v1/users` | 需登录 | 当前用户信息 |
| 角色 | `/api/v1/roles` | 管理员 | 角色管理 |
| 文章（公开） | `/api/v1/articles` | 公开 | 列表、详情、相关推荐 |
| 文章（管理） | `/api/v1/admin/articles` | 管理员 | CRUD + 发布 + 归档 + 重建向量 |
| 分类（公开） | `/api/v1/categories` | 公开 | 树形、列表、slug 查询 |
| 分类（管理） | `/api/v1/admin/categories` | 管理员 | CRUD + 移动 |
| 标签（公开） | `/api/v1/tags` | 公开 | 列表、热门、slug 查询 |
| 标签（管理） | `/api/v1/admin/tags` | 管理员 | CRUD + 批量 + 合并 |
| 壁纸（管理） | `/api/v1/admin/wallpapers` | 管理员 | 必应壁纸列表 |
| 评论 | `/api/v1/comments` | 公开（可评论）| 评论树、发表、回复、点赞 |
| 评论（管理） | `/api/v1/admin/comments` | 管理员 | 评论列表 |
| 评论审核 | `/api/v1/comments/audit` | 管理员 | 通过、拒绝、删除 |
| 评论举报 | `/api/v1/comments/reports` | 管理员 | 举报列表、审核 |
| 文件 | `/api/v1/files` | 需登录 | 预签名、确认、列表、删除、访问 URL |
| AI 问答 | `/api/v1/ai` | 公开 | RAG 问答 |
| Ops | `/api/v1/ops` | 管理员（ROLE_ADMIN）| SSE 流式运维对话 |
| Webhook | `/api/v1/webhooks` | 公开（HMAC 验签）| GitHub CI 事件接收 |
| Actuator | `/actuator` | 公开（白名单）| 健康、指标、Prometheus |
| Swagger UI | `/swagger-ui.html` | 公开 | API 文档 |

---

## 架构设计

### 多模块分层结构

```
blog-application（启动模块）
   │── blog-modules/blog-module-article
   │── blog-modules/blog-module-comment
   │── blog-modules/blog-module-file
   │── blog-modules/blog-module-ai
   │── blog-modules/blog-module-system
   └── blog-common（公共依赖）
```

每个业务模块内部遵循**领域驱动分层**：

```
controller/     ← REST API 入口（VO/DTO 出入参，统一 Result<T> 响应）
service/        ← 业务逻辑（接口 + 实现分离）
domain/         ← 领域对象（Entity + 状态机 + 领域事件）
converter/      ← MapStruct 对象映射（编译期生成，零反射）
infrastructure/ ← 基础设施（Mapper、Redis、OSS、SSH 等）
metrics/        ← Micrometer 自定义指标
```

### 认证与安全

```
HTTP Request
    │
    ▼
JwtAuthenticationFilter（从 Authorization Header 提取并验证 JWT）
    │
    ├─ Token 合法 → 填充 SecurityContext → 继续
    └─ Token 非法 → 401 Unauthorized
    
SecurityConfig（白名单路径直接放行）
    │
    └─ @PreAuthorize("hasRole('ADMIN')")  控制管理员专属接口
```

**白名单**（无需 JWT 即可访问）：
- 公开读接口：`/api/v1/articles/**`、`/api/v1/categories/**`、`/api/v1/tags/**`、`/api/v1/comments/tree`
- AI 问答：`/api/v1/ai/**`
- Webhook：`/api/v1/webhooks/**`（HMAC-SHA256 验签）
- Actuator、Swagger UI

### 缓存策略

| 数据 | 缓存类型 | 失效策略 |
|:---|:---|:---|
| 文章详情 | Redis | 发布 / 编辑后主动删除 |
| 评论树 | Redis | 新增 / 审核后主动删除 |
| 热门标签 | Caffeine（本地） | TTL 过期 |
| 分类树 | Caffeine（本地） | TTL 过期 |
| API 响应 | — | 不缓存（动态数据） |

### 可观测性

```
应用指标 ──────► Prometheus（/actuator/prometheus）
                      │
                      ▼
              Grafana 仪表盘（Alloy 采集）

结构化日志 ───► JSON 文件（logstash-logback-encoder）
                      │
                      ▼
              Alloy → Loki → Grafana

链路追踪 ─────► OpenTelemetry Agent（-javaagent）
                      │
                      ▼
              OTLP → Tempo → Grafana

实例监控 ─────► Spring Boot Admin（端口 9000）
```

---

## 配置说明

### Profile 说明

| Profile | 启用场景 | 数据库/Redis 地址 |
|:---|:---|:---|
| `dev`（默认） | 本地开发（application-dev.yaml） | `127.0.0.1` |
| `prod` | Docker 生产（application-prod.yaml） | `mysql` / `redis`（Compose 服务名） |

### 关键环境变量

| 变量 | 说明 | 是否必填 |
|:---|:---|:---|
| `DB_USERNAME` | MySQL 用户名 | 生产必填 |
| `DB_PASSWORD` | MySQL 密码 | 生产必填 |
| `REDIS_PASSWORD` | Redis 密码 | 可选 |
| `JWT_SECRET` | JWT 签名密钥（≥256 bit）| 生产必填 |
| `JWT_EXPIRATION` | Token 过期时间（ms，默认 7200000）| 可选 |
| `DASHSCOPE_API_KEY` | 通义千问 DashScope API Key | AI 功能必填 |
| `QDRANT_HOST` | Qdrant Cloud 集群地址 | AI 功能必填 |
| `QDRANT_PORT` | Qdrant gRPC 端口（默认 6334）| AI 功能必填 |
| `QDRANT_API_KEY` | Qdrant Cloud API Key | AI 功能必填 |
| `BITIFUL_ACCESS_KEY` | Bitiful OSS Access Key | 文件功能必填 |
| `BITIFUL_SECRET_KEY` | Bitiful OSS Secret Key | 文件功能必填 |
| `BITIFUL_BUCKET` | Bitiful Bucket 名 | 文件功能必填 |
| `OSS_TYPE` | OSS 类型（默认 BITIFUL）| 可选 |
| `OPS_SSH_HOST` | 目标 VPS IP / 域名 | Ops 功能必填 |
| `OPS_SSH_USER` | SSH 用户名（建议最小权限）| Ops 功能必填 |
| `OPS_SSH_KEY_PATH` | SSH 私钥路径（容器内挂载）| Ops 功能必填 |
| `OPS_DEPLOY_DIR` | 远端 compose.yml 所在目录 | Ops 功能必填 |
| `OPS_WEBHOOK_SECRET` | GitHub Webhook 签名密钥 | 本地可留空 |

---

## 本地开发

### 前置条件

- JDK 21+（推荐使用 [vfox](https://vfox.linspiration.dev/) 管理，见 `.vfox.toml`）
- Maven 3.9+
- MySQL 9.x（本地或 Docker）
- Redis（本地或 Docker）

### 快速启动

```bash
# 1. 初始化数据库（首次）
# 执行 blog-application/src/main/resources/db/ 目录下的 SQL 脚本

# 2. 配置环境变量（本地开发最小集）
export DASHSCOPE_API_KEY=sk-xxx          # AI 功能（可选）
export QDRANT_API_KEY=your-qdrant-key    # AI 功能（可选）
export QDRANT_HOST=your-qdrant-host      # AI 功能（可选）

# 3. 编译打包（跳过测试）
mvn clean package -DskipTests

# 4. 运行（dev profile，连接本地 MySQL/Redis）
java -jar blog-application/target/blog-application-*.jar

# 或直接在 IDEA 中运行 BlogApplication.java
```

> 本地开发默认激活 `dev` profile，连接 `127.0.0.1:3306` 和 `127.0.0.1:6379`。

### P6Spy SQL 监控

`dev` 环境自动启用 P6Spy，在控制台输出每条执行的完整 SQL 语句及耗时。配置文件：`spy.properties`。

> ⚠️ **生产环境必须禁用**，通过 `prod` profile 自动关闭。

### API 文档（Swagger UI）

启动后访问：http://localhost:8080/swagger-ui.html

---

## 构建与部署

### Docker 4 阶段构建

| 阶段 | 基础镜像 | 作用 |
|:---|:---|:---|
| `builder` | `maven:3.9-eclipse-temurin-21-alpine` | 编译源码，执行 `mvn package -DskipTests` |
| `extractor` | `eclipse-temurin:21-jre-alpine` | 用 `layertools` 将 fat jar 拆分为 4 个独立 Docker 层 |
| `runtime` | `eclipse-temurin:21-jre-alpine` | 最小 JRE，分层复制 jar，挂载 OTel Java Agent |

Spring Boot 分层 jar（`layertools`）优化构建缓存：

```
dependencies/          → 第三方库（~50MB，极少变，永久缓存）
spring-boot-loader/    → Spring Boot 加载器（不变）
snapshot-dependencies/ → 快照依赖（偶尔变）
application/           → 业务代码（< 1MB，改动频繁）
```

只修改业务代码时，Docker 只重建最后一层（< 1MB），大幅加速 CI/CD 构建。

### JVM 启动参数

```
-XX:+UseContainerSupport      让 JVM 感知 Docker 内存限制
-XX:MaxRAMPercentage=75.0     JVM 最多使用容器分配内存的 75%
-Djava.security.egd=...       加快 SecureRandom 初始化，避免启动卡顿
```

### GitHub Actions CI/CD

推送到 `main` 分支时自动触发：

```
push to main
     │
     ▼
Checkout → Docker Login → Setup Buildx → Build & Push
                                              │
                          标签：latest + sha-<commit>
                          缓存：GitHub Actions Cache（GHA）
```

附加工作流：
- **CodeQL**：每次推送自动扫描 Java 安全漏洞
- **Qodana**：代码质量检查（重复代码、复杂度等）
- **Admin Server 镜像**：独立工作流发布 Spring Boot Admin 镜像

**所需 GitHub Secrets / Variables：**

| 名称 | 类型 | 说明 |
|:---|:---|:---|
| `DOCKERHUB_USERNAME` | Variable | Docker Hub 用户名 |
| `DOCKERHUB_TOKEN` | Secret | Docker Hub Access Token |

### 运行 Docker 镜像

```bash
docker run -d \
  -p 8080:8080 \
  -e DB_USERNAME=xxx \
  -e DB_PASSWORD=xxx \
  -e REDIS_PASSWORD=xxx \
  -e JWT_SECRET=your-256bit-secret \
  -e DASHSCOPE_API_KEY=sk-xxx \
  -e QDRANT_HOST=xxx \
  -e QDRANT_API_KEY=xxx \
  -e BITIFUL_ACCESS_KEY=xxx \
  -e BITIFUL_SECRET_KEY=xxx \
  liusxml/personal-blog-backend:latest
```

### SSH Key 挂载（Ops Agent）

Ops Agent 通过 SSH 连接远端 VPS，需将宿主机私钥挂载到容器：

```yaml
# compose.yml 配置示例
backend:
  volumes:
    - ~/.ssh/id_ed25519:/app/ssh/id_ed25519:ro
  environment:
    OPS_SSH_KEY_PATH: /app/ssh/id_ed25519
```

---

## 开发规范

### 统一响应格式

所有接口（SSE 接口除外）返回统一 `Result<T>` 包装：

```json
{
  "code": "200",
  "message": "操作成功",
  "data": { ... },
  "success": true
}
```

### 异常处理

通过 `@RestControllerAdvice` 全局捕获，业务异常抛 `BusinessException`（含错误码），框架异常自动映射为标准响应。

### MapStruct 对象映射

所有 Entity ↔ VO/DTO 转换使用 MapStruct，**禁止**使用 BeanUtils 反射复制。MapStruct 在编译期生成代码，无运行时性能开销。

### 模块依赖规则

- `blog-common` 不依赖任何业务模块
- 业务模块间禁止相互依赖（通过 `blog-application` 聚合）
- `*-api` 子模块只包含 VO、DTO、接口定义，不含实现
- 架构规则由 ArchUnit 测试用例自动验证（`src/test/`）

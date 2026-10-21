# syntax=docker/dockerfile:1
# ==============================================================================
# 第一阶段：构建阶段（Builder）
# 使用包含 Maven 和 JDK 21 的镜像来编译项目、打包成 fat jar
# ==============================================================================
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# ── 优化：先只复制所有 pom.xml 文件（利用 Docker 层缓存）─────────────────
# 包含全部聚合模块与子模块的 pom.xml，确保 Maven 反应堆拓扑完整
COPY pom.xml .
COPY blog-common/pom.xml blog-common/
COPY blog-mcp-server/pom.xml blog-mcp-server/
COPY blog-modules/pom.xml blog-modules/
COPY blog-modules/blog-module-system/pom.xml blog-modules/blog-module-system/
COPY blog-modules/blog-module-system/blog-system-api/pom.xml blog-modules/blog-module-system/blog-system-api/
COPY blog-modules/blog-module-system/blog-system-service/pom.xml blog-modules/blog-module-system/blog-system-service/
COPY blog-modules/blog-module-article/pom.xml blog-modules/blog-module-article/
COPY blog-modules/blog-module-article/blog-article-api/pom.xml blog-modules/blog-module-article/blog-article-api/
COPY blog-modules/blog-module-article/blog-article-service/pom.xml blog-modules/blog-module-article/blog-article-service/
COPY blog-modules/blog-module-comment/pom.xml blog-modules/blog-module-comment/
COPY blog-modules/blog-module-comment/blog-comment-api/pom.xml blog-modules/blog-module-comment/blog-comment-api/
COPY blog-modules/blog-module-comment/blog-comment-service/pom.xml blog-modules/blog-module-comment/blog-comment-service/
COPY blog-modules/blog-module-file/pom.xml blog-modules/blog-module-file/
COPY blog-modules/blog-module-file/blog-file-api/pom.xml blog-modules/blog-module-file/blog-file-api/
COPY blog-modules/blog-module-file/blog-file-service/pom.xml blog-modules/blog-module-file/blog-file-service/
COPY blog-modules/blog-module-ai/pom.xml blog-modules/blog-module-ai/
COPY blog-modules/blog-module-ai/blog-ai-api/pom.xml blog-modules/blog-module-ai/blog-ai-api/
COPY blog-modules/blog-module-ai/blog-ai-service/pom.xml blog-modules/blog-module-ai/blog-ai-service/
COPY blog-application/pom.xml blog-application/
COPY blog-admin-server/pom.xml blog-admin-server/

# 复制全部源代码
COPY . .

# ── 优化：RUN --mount=type=cache 挂载 Maven 本地仓库并直接分层解压 ─────────
# 1. 通过 -pl blog-application -am 仅编译主应用及其依赖模块，跳过无关的 blog-admin-server
# 2. 使用 Spring Boot 3.5 官方推荐的 -Djarmode=tools 分层提取（替代已废弃的 layertools）：
#    - dependencies/          → 第三方正式依赖库（lib/*.jar，极少变动，永久缓存）
#    - spring-boot-loader/    → Spring Boot 加载器
#    - snapshot-dependencies/ → 快照依赖
#    - application/           → 业务代码轻量入口 app.jar（含 Class-Path 索引，< 1MB）
RUN --mount=type=cache,target=/root/.m2 \
    mvn package -DskipTests -B -pl blog-application -am && \
    cp blog-application/target/blog-application-*.jar app.jar && \
    java -Djarmode=tools -jar app.jar extract --layers --destination extracted

# ==============================================================================
# 第二阶段：运行阶段（Runtime）
# 最小 JRE Alpine 镜像，分层复制解压后的产物
# ==============================================================================
FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app

# ── 安全配置：创建非 root 用户（Spring Boot 官方指南要求）─────────────────
# ⚠️ 必须在 ADD --chown=appuser 之前创建用户，否则构建报错
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# ── OpenTelemetry Java Agent（无侵入链路追踪）────────────────────────────
# 版本锁定（避免 latest 带来的不可控升级风险）
# 当前版本：v2.32.0（最新稳定版，全面兼容 Java 21 与 Spring Boot 3.5）
# 升级时修改版本号，参考：https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases
# 运行时通过 compose.yml 的 JAVA_TOOL_OPTIONS 环境变量激活，无需修改 ENTRYPOINT
ADD --chown=appuser:appgroup \
    https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.32.0/opentelemetry-javaagent.jar \
    /app/opentelemetry-javaagent.jar

# ── 分层复制（从变化最少 → 变化最频繁的顺序）────────────────────────────
# 这样当你只改了业务代码时，Docker 只重建最后一层（< 1MB），
# 前三层（第三方库等，>50MB）会被完整缓存，大幅加速 CI/CD 构建
COPY --from=builder --chown=appuser:appgroup /build/extracted/dependencies/ ./
COPY --from=builder --chown=appuser:appgroup /build/extracted/spring-boot-loader/ ./
COPY --from=builder --chown=appuser:appgroup /build/extracted/snapshot-dependencies/ ./
COPY --from=builder --chown=appuser:appgroup /build/extracted/application/ ./

# 切换到非 root 用户运行
USER appuser

# 声明容器监听 8080 端口
EXPOSE 8080

# ── 启动命令 ─────────────────────────────────────────────────────────────
# Spring Boot 3.5 jarmode=tools 提取后直接使用标准 -jar app.jar 启动（比旧版 JarLauncher 启动更快且支持 CDS）
# -XX:+UseContainerSupport   让 JVM 感知 Docker 的内存限制
# -XX:MaxRAMPercentage=75.0  JVM 最多使用容器分配内存的 75%
# -Djava.security.egd        加快 SecureRandom 初始化，避免启动卡顿
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", \
  "app.jar", \
  "--spring.profiles.active=prod"]

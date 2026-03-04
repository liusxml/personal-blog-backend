# syntax=docker/dockerfile:1
# ==============================================================================
# 第一阶段：构建阶段（Builder）
# 使用包含 Maven 和 JDK 21 的镜像来编译项目、打包成 fat jar
# ==============================================================================
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# ── 优化：先只复制所有 pom.xml 文件（利用 Docker 层缓存）─────────────────
# 如果 pom.xml 没有改变，Docker 复用这一层，不重新下载依赖
COPY pom.xml .
COPY blog-common/pom.xml blog-common/
COPY blog-modules/pom.xml blog-modules/
COPY blog-modules/blog-module-system/pom.xml blog-modules/blog-module-system/
COPY blog-modules/blog-module-article/pom.xml blog-modules/blog-module-article/
COPY blog-modules/blog-module-comment/pom.xml blog-modules/blog-module-comment/
COPY blog-modules/blog-module-file/pom.xml blog-modules/blog-module-file/
COPY blog-application/pom.xml blog-application/
COPY blog-admin-server/pom.xml blog-admin-server/

# 复制全部源代码
COPY . .

# ── 优化：RUN --mount=type=cache 挂载 Maven 本地仓库 ─────────────────────
# 官方 Dockerfile 文档推荐的最优缓存方式：即使 pom.xml 改变，已下载的依赖仍然复用
RUN --mount=type=cache,target=/root/.m2 \
    mvn package -DskipTests -B

# ==============================================================================
# 第二阶段：解压阶段（Extractor）
# Spring Boot 官方指南推荐：使用 layertools 将 fat jar 拆分为 4 个独立的层
# 目的：让 Docker 层缓存精细化到"依赖层"和"业务代码层"
# ==============================================================================
FROM eclipse-temurin:21-jre-alpine AS extractor

WORKDIR /build

# 从第一阶段复制 fat jar
COPY --from=builder /build/blog-application/target/blog-application-*.jar app.jar

# 使用 Spring Boot layertools 将 fat jar 解压为 4 个层：
#   dependencies/          → 第三方库（很少变，~50MB，永久缓存）
#   spring-boot-loader/    → Spring Boot 加载器（不变）
#   snapshot-dependencies/ → 快照依赖（偶尔变）
#   application/           → 你的业务代码（经常变，< 1MB）
RUN java -Djarmode=layertools -jar app.jar extract --destination extracted

# ==============================================================================
# 第三阶段：运行阶段（Runtime）
# 最小 JRE Alpine 镜像，分层复制 jar 内容
# ==============================================================================
FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app

# ── 安全配置：创建非 root 用户（Spring Boot 官方指南要求）─────────────────
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# ── 分层复制（从变化最少 → 变化最频繁的顺序）────────────────────────────
# 这样当你只改了业务代码时，Docker 只重建最后一层（< 1MB），
# 前三层（第三方库等，>50MB）会被完整缓存，大幅加速 CI/CD 构建
COPY --from=extractor --chown=appuser:appgroup /build/extracted/dependencies/ ./
COPY --from=extractor --chown=appuser:appgroup /build/extracted/spring-boot-loader/ ./
COPY --from=extractor --chown=appuser:appgroup /build/extracted/snapshot-dependencies/ ./
COPY --from=extractor --chown=appuser:appgroup /build/extracted/application/ ./

# 切换到非 root 用户运行
USER appuser

# 声明容器监听 8080 端口
EXPOSE 8080

# ── 启动命令 ─────────────────────────────────────────────────────────────
# 分层 jar 不再使用 java -jar，改用 JarLauncher（Spring Boot 官方推荐）
# -XX:+UseContainerSupport   让 JVM 感知 Docker 的内存限制
# -XX:MaxRAMPercentage=75.0  JVM 最多使用容器分配内存的 75%
# -Djava.security.egd        加快 SecureRandom 初始化，避免启动卡顿
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "org.springframework.boot.loader.launch.JarLauncher", \
  "--spring.profiles.active=prod"]

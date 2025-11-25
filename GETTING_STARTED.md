# 🚀 快速开始指南

本文档将帮助你快速启动 Personal Blog Backend 项目。

---

## 📋 开始工作前的准备清单

### 1️⃣ 环境验证

确保你已安装以下工具：

```bash
# 检查 Java 版本（需要 21+）
java -version

# 检查 Maven 版本（需要 3.6+）
mvn -version

# 检查 Docker（可选，但推荐）
docker --version
docker-compose --version
```

**当前环境状态**：
- ✅ Java 21.0.8 (OpenJDK Temurin)
- ✅ Maven 3.9.9
- ⚠️ Docker 需要启动

---

### 2️⃣ 启动 MySQL 数据库

#### 方案 A：使用 Docker（推荐）

1. **启动 Docker Desktop 或 OrbStack**
   ```bash
   # 如果使用 OrbStack（macOS）
   open -a OrbStack
   
   # 或者启动 Docker Desktop
   open -a Docker
   ```

2. **启动 MySQL 容器**
   ```bash
   # 在项目根目录执行
   docker-compose up -d mysql
   
   # 查看容器状态
   docker-compose ps
   
   # 查看日志
   docker-compose logs -f mysql
   ```

3. **验证数据库连接**
   ```bash
   # 进入 MySQL 容器
   docker exec -it blog-mysql mysql -uroot -ps3cr3t_r00t_p@ssw0rd
   
   # 在 MySQL 中执行
   SHOW DATABASES;
   USE blog_db;
   SHOW TABLES;
   ```

#### 方案 B：使用本地 MySQL

如果你已经安装了本地 MySQL：

1. **创建数据库**
   ```sql
   CREATE DATABASE blog_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```

2. **执行初始化脚本**
   ```bash
   mysql -uroot -p blog_db < blog-application/src/main/resources/db/v1.0.0_init_schema.sql
   ```

3. **更新配置文件**（如果需要）
   
   修改 `blog-application/src/main/resources/application.yaml`：
   ```yaml
   spring:
     datasource:
       url: jdbc:mysql://127.0.0.1:3306/blog_db?...
       username: your_username
       password: your_password
   ```

---

### 3️⃣ 构建项目

在项目根目录执行：

```bash
# 清理并编译所有模块
mvn clean install

# 跳过测试（如果测试失败）
mvn clean install -DskipTests
```

**预期输出**：
```
[INFO] personal-blog-backend (Root POM) .................. SUCCESS
[INFO] blog-common ........................................ SUCCESS
[INFO] blog-modules ....................................... SUCCESS
[INFO] blog-system-api .................................... SUCCESS
[INFO] blog-system-service ................................ SUCCESS
[INFO] blog-article-api ................................... SUCCESS
[INFO] blog-article-service ............................... SUCCESS
[INFO] blog-comment-api ................................... SUCCESS
[INFO] blog-comment-service ............................... SUCCESS
[INFO] blog-file-api ...................................... SUCCESS
[INFO] blog-file-service .................................. SUCCESS
[INFO] blog-application ................................... SUCCESS
[INFO] blog-admin-server .................................. SUCCESS
```

---

### 4️⃣ 启动应用

#### 方式 1：使用 Maven 插件（推荐，支持热重载）

```bash
# 在项目根目录执行
mvn spring-boot:run -pl blog-application

# 或者进入 blog-application 目录
cd blog-application
mvn spring-boot:run
```

#### 方式 2：直接运行 JAR 文件

```bash
# 先打包
mvn clean package -pl blog-application -am

# 运行
java -jar blog-application/target/blog-application-1.0-SNAPSHOT.jar
```

#### 方式 3：在 IDEA 中运行

1. 打开 `blog-application/src/main/java/com/blog/BlogApplication.java`
2. 右键 → Run 'BlogApplication'

---

### 5️⃣ 验证启动成功

应用启动后，访问以下端点验证：

#### 业务端口（8080）
- **API 文档**: http://localhost:8080/swagger-ui.html
- **API JSON**: http://localhost:8080/v3/api-docs

#### 管理端口（8081）
- **健康检查**: http://localhost:8081/actuator/health
- **应用信息**: http://localhost:8081/actuator/info
- **性能指标**: http://localhost:8081/actuator/metrics
- **Prometheus**: http://localhost:8081/actuator/prometheus

**预期健康检查响应**：
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
    "diskSpace": { "status": "UP" },
    "ping": { "status": "UP" }
  }
}
```

---

## 🔧 常见问题排查

### ❌ 数据库连接失败

**错误信息**：
```
Communications link failure
```

**解决方案**：
1. 确认 MySQL 正在运行：`docker-compose ps` 或 `mysql -uroot -p`
2. 检查端口没有被占用：`lsof -i :3306`
3. 验证数据库配置（URL、用户名、密码）

---

### ❌ 端口冲突

**错误信息**：
```
Port 8080 is already in use
```

**解决方案**：
```bash
# 查找占用端口的进程
lsof -i :8080

# 杀死进程
kill -9 <PID>

# 或者修改 application.yaml 中的端口
server:
  port: 8081
```

---

### ❌ Maven 构建失败

**常见原因**：
1. **网络问题**：无法下载依赖
   ```bash
   # 使用阿里云镜像（编辑 ~/.m2/settings.xml）
   ```

2. **Lombok/MapStruct 冲突**
   - 确保 IDE 已安装 Lombok 插件
   - 执行 `mvn clean install` 重新生成代码

3. **测试失败**
   ```bash
   # 跳过测试
   mvn clean install -DskipTests
   ```

---

## 🎯 下一步

项目启动成功后，你可以：

1. **查看 API 文档**：http://localhost:8080/swagger-ui.html
2. **创建测试用户**：通过 `/api/auth/register` 注册
3. **获取 JWT Token**：通过 `/api/auth/login` 登录
4. **测试文章 API**：使用 JWT Token 访问受保护的端点
5. **启动监控**：访问 Spring Boot Admin（如果需要）

---

## 📚 相关文档

- [README.md](./README.md) - 项目架构和技术栈
- [CLAUDE.md](./CLAUDE.md) - AI 辅助开发指南
- [API 文档](http://localhost:8080/swagger-ui.html) - 运行时可用

---

## 🆘 需要帮助？

如果遇到问题：
1. 检查日志：`logs/` 目录或控制台输出
2. 查看 Actuator 健康状态：http://localhost:8081/actuator/health
3. 联系项目维护者或提交 Issue

---

**祝你开发顺利！** 🚀

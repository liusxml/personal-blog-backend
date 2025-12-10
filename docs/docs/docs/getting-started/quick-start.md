---
sidebar_position: 2
---

# 快速启动

只需 **5 分钟**，即可在本地运行 Personal Blog Backend 并访问 API 文档。

## 📋 环境要求

在开始之前，请确保你已经安装：

- ☕ **JDK 21** 或更高版本 ([下载地址](https://openjdk.org/projects/jdk/21/))
- 📦 **Maven 3.6+** ([下载地址](https://maven.apache.org/download.cgi))
- 🗄️ **MySQL 8.0+** （需要一个正在运行的数据库实例）
- 🔧 **Git**（用于克隆项目）

:::tip 验证环境
在终端运行以下命令验证安装：
```bash
java -version    # 应显示 java 21.x.x
mvn -version     # 应显示 Apache Maven 3.6+
mysql --version  # 应显示 MySQL 8.0+
```
:::

## 🚀 启动步骤

### 1️⃣ 克隆项目

```bash
git clone https://github.com/liusxml/personal-blog-backend.git
cd personal-blog-backend
```

### 2️⃣ 配置数据库

在 `blog-application/src/main/resources/` 目录下创建 `application.yml` 文件：

```yaml title="blog-application/src/main/resources/application.yml"
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/blog_db?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: your_username
    password: your_password
    driver-class-name: com.mysql.cj.jdbc.Driver

  # Redis 配置（可选，用于缓存）
  data:
    redis:
      host: localhost
      port: 6379
      password:  # 如果 Redis 没有密码，留空

# MyBatis-Plus 配置
mybatis-plus:
  global-config:
    # 自动维护 DDL（开发/测试环境）
    db-config:
      # 字段策略：非空判断
      insert-strategy: not_null
      update-strategy: not_null
    # Auto DDL 自动建表
    enable-auto-ddl: true
```

:::warning 重要提示
- 请将 `your_username` 和 `your_password` 替换为你的 MySQL 用户名和密码
- 数据库 `blog_db` 需要手动创建，或在 URL 中添加 `createDatabaseIfNotExist=true`
- **MyBatis-Plus Auto DDL** 会根据实体类自动创建/更新表结构（仅开发环境使用）
- **Redis 是可选的**，如果没有安装 Redis，可以跳过 Redis 配置，应用会自动禁用缓存
:::

### 3️⃣ 构建项目

在项目根目录执行：

```bash
mvn clean install
```

:::info 首次构建
首次构建可能需要 2-3 分钟，Maven 需要下载依赖包。
:::

### 4️⃣ 启动应用

有两种方式启动应用：

**方式一：使用 Spring Boot Maven 插件（推荐）**

```bash
mvn spring-boot:run -pl blog-application
```

**方式二：运行打包后的 JAR**

```bash
# 先打包
mvn package -DskipTests

# 运行 JAR
java -jar blog-application/target/blog-application-*.jar
```

:::tip 开发建议
开发时推荐使用方式一，支持代码热加载，修改后快速生效。
:::

### 5️⃣ 验证启动

当你看到以下日志，说明启动成功：

```
Started BlogApplication in X.XXX seconds (process running for X.XXX)
```

## 🎉 访问应用

应用启动后，你可以访问：

### Swagger UI（API 文档）
🔗 **http://localhost:8080/swagger-ui.html**

这是一个交互式的 API 文档页面，你可以：
- 查看所有可用的 API 接口
- 在线测试 API（无需 Postman）
- 查看请求/响应示例

### 健康检查
🔗 **http://localhost:8080/actuator/health**

应返回：
```json
{
  "status": "UP"
}
```

### API 基础路径
🔗 **http://localhost:8080/api/**

所有业务 API 都在 `/api` 路径下。

## 🧪 测试第一个 API

让我们快速测试一下用户注册接口：

### 使用 Swagger UI（推荐新手）

1. 打开 http://localhost:8080/swagger-ui.html
2. 找到 `认证管理` 分组
3. 展开 `POST /auth/register` 接口
4. 点击 **Try it out**
5. 输入以下 JSON：
```json
{
  "username": "testuser",
  "password": "Password123!",
  "email": "test@example.com",
  "nickname": "Test User"
}
```
6. 点击 **Execute**

### 使用 curl

```bash
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "Password123!",
    "email": "test@example.com",
    "nickname": "Test User"
  }'
```

如果成功，你会收到类似的响应：

```json
{
  "code": 0,
  "message": "Success",
  "data": {
    "id": 1,
    "username": "testuser",
    "email": "test@example.com",
    "nickname": "Test User",
    "status": 1
  }
}
```

### 🔐 测试登录和认证

注册成功后，你可以测试登录：

```bash
# 登录获取 Token
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "Password123!"
  }'

# 返回结果包含 Token
{
  "code": 0,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 86400
  }
}

# 使用 Token 访问受保护的 API
curl -X GET http://localhost:8080/api/users/me \
  -H "Authorization: Bearer {your_token}"
```

## 🔍 常见问题

### ❌ 启动失败：数据库连接错误

**错误信息**：`com.mysql.cj.jdbc.exceptions.CommunicationsException`

**解决方案**：
1. 检查 MySQL 是否正在运行：`sudo systemctl status mysql`（Linux）或查看服务（Windows/Mac）
2. 验证 `application.yml` 中的用户名、密码是否正确
3. 确认数据库端口是 3306（或修改配置中的端口）

### ❌ 端口 8080 已被占用

**错误信息**：`***************************`APPLICATION FAILED TO START***************************`

**解决方案**：
在 `application.yml` 中修改端口：
```yaml
server:
  port: 8081  # 或其他未被占用的端口
```

### ❌ 构建失败：依赖下载超时

**解决方案**：
配置国内 Maven 镜像，编辑 `~/.m2/settings.xml`：
```xml
<mirrors>
  <mirror>
    <id>aliyun</id>
    <mirrorOf>central</mirrorOf>
    <url>https://maven.aliyun.com/repository/public</url>
  </mirror>
</mirrors>
```

## 🎯 下一步

恭喜！你已经成功启动了应用。接下来可以：

- 📖 [了解架构设计](../architecture/overview) - 深入理解系统架构
- 📚 [API 参考文档](../api/overview) - 查看完整的 REST API
- 🛠️ [学习开发规范](../development/standards) - 如何贡献代码
- 🧪 [编写测试](../testing/overview) - 保证代码质量

---

有问题？欢迎在 [GitHub Issues](https://github.com/liusxml/personal-blog-backend/issues) 提问！

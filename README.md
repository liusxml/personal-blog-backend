# 🚀 Personal Blog Backend

[![Java](https://img.shields.io/badge/Java-21-blue.svg?logo=openjdk)](https://openjdk.java.net/)[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5.6-brightgreen.svg?logo=spring)](https://spring.io/projects/spring-boot)[![Maven](https://img.shields.io/badge/Maven-3.6+-orange.svg?logo=apache-maven)](https://maven.apache.org/)[![License](https://img.shields.io/badge/license-MIT-lightgrey.svg)](https://opensource.org/licenses/MIT)

这是一个基于 **Spring Boot 3 + Java 21** 构建的现代化、分层、模块化的个人博客后端系统。项目采用 Maven
多模块架构，遵循“高内聚、低耦合”的设计原则，实现了清晰的业务边界和服务分离，使其易于维护、测试和扩展。

---

### ✨ 核心功能

- **文章管理 (Article Module)**: 提供文章的发布、编辑、删除和查询等全套 RESTful API。
- **评论系统 (Comment Module)**: 支持对文章进行评论和回复。
- **系统管理 (System Module)**: 包含基于 JWT 的用户认证、角色权限等基础管理功能。
- **文件服务 (File Module)**: 提供统一的文件上传和管理能力。
- **生产就绪 (Production-Ready)**: 集成 Actuator 提供健康检查、性能指标等监控端点。

---

### 🛠️ 技术栈

基于 `pom.xml` 的最终技术选型：

| 分类         | 技术                      | 版本 (`pom.xml` 定义)     | 作用                                  |
|:-----------|:------------------------|:----------------------|:------------------------------------|
| **核心框架**   | Spring Boot             | `3.5.6`               | 快速构建生产级 Spring 应用                   |
| **开发语言**   | Java                    | `21`                  | 项目主要开发语言                            |
| **构建工具**   | Apache Maven            | `3.6+`                | 依赖管理和项目构建                           |
| **持久层**    | MyBatis-Plus            | `3.5.14`              | ORM 框架，简化数据库操作                      |
| **数据库**    | MySQL                   | `9.4.0` (Connector/J) | 关系型数据库存储                            |
| **数据库迁移**  | Flyway                  | `11.13.1`             | 管理和版本化数据库 Schema 变更                 |
| **安全与认证**  | Spring Security + JWT   | `0.13.0` (jjwt)       | 实现无状态认证和授权                          |
| **API 文档** | SpringDoc OpenAPI       | `2.8.13`              | 自动生成和展示 RESTful API 文档 (Swagger UI) |
| **缓存**     | Spring Cache + Caffeine | `3.2.2`               | 高性能的本地缓存，提升应用响应速度                   |
| **开发工具**   | Lombok, MapStruct       | -                     | 简化样板代码，如 Getter/Setter 和对象转换        |
| **生产监控**   | Spring Boot Actuator    | -                     | 提供应用监控和管理端点                         |
| **常用工具**   | Apache Commons, Guava   | -                     | 提供丰富的工具类库                           |

---

### 🏗️ 项目架构

本项目采用 Maven 多模块和分层设计，确保了关注点分离 (Separation of Concerns)。

```
personal-blog-backend (根模块, packaging: pom)
│
├── pom.xml                   # 根POM，使用 dependencyManagement & pluginManagement 统一管理全局版本
│
├── blog-application          # 🚀 应用启动模块 (聚合所有 service 实现并启动)
│
├── blog-common               # 🔧 公共工具模块 (工具类, 全局常量, 通用 DTO, 异常处理等)
│
└── blog-modules              # 📦 核心业务模块集合 (packaging: pom)
    │
    ├── blog-article          # 📄 文章业务
    │   ├── blog-article-api      #    - API 接口定义 (DTOs, Enums, Constants)
    │   └── blog-article-service  #    - Service 服务实现 (依赖 api, 实现业务逻辑)
    │
    ├── blog-comment          # 💬 评论业务
    │   ├── blog-comment-api
    │   └── blog-comment-service
    │
    ├── blog-system           # 🛡️ 系统管理业务
    │   ├── blog-system-api
    │   └── blog-system-service
    │
    └── blog-file             # 📁 文件管理业务
        ├── blog-file-api
        └── blog-file-service
```

* **分层设计**: 每个业务模块内部都拆分为 `api` 和 `service` 两个子模块。
* **`-api` 模块**: 只包含 POJO（DTOs, VO等）、枚举和接口定义。它非常轻量，可以被其他任何模块安全地依赖，而不会引入复杂的业务逻辑。
* **`-service` 模块**: 包含业务逻辑的具体实现 (Service, Mapper)。它依赖于自己的 `api` 模块和 `blog-common`。
* **依赖流向**: `blog-application` -> `*-service` -> `*-api` -> `blog-common`。这种单向依赖关系确保了模块间的隔离，避免了循环依赖。
* **统一管理**: 根 `pom.xml` 作为“单一事实来源”，集中管理所有依赖版本，确保项目整体的稳定性和一致性。

---

### 🚀 快速开始

**环境要求**:

* JDK 21 或更高版本
* Maven 3.6+
* 一个正在运行的 MySQL 8.0+ 数据库实例

**步骤**:

1. **克隆项目到本地**

 ```bash
 git clone https://github.com/your-username/personal-blog-backend.git
 cd personal-blog-backend
 ```

2. **配置数据库**

* 在 `blog-application` 模块的 `src/main/resources/` 目录下创建 `application.yml`。
* 配置 `spring.datasource` 指向你的数据库。Flyway 将在应用启动时自动执行 `src/main/resources/db/migration` 下的 SQL
  脚本来初始化或更新数据库结构。

 ```yaml
 spring:
 datasource:
 url: jdbc:mysql://localhost:3306/your_blog_db?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
 username: your_username
 password: your_password
 ```

3. **使用 Maven 构建项目**

 ```bash
 # 该命令会编译所有模块并将其安装到本地 Maven 仓库
 mvn clean install
 ```

4. **运行应用**

 ```bash
 # 方式一: (推荐) 通过 Spring Boot 插件直接运行，支持热部署
 # 在项目根目录执行:
 mvn spring-boot:run -pl blog-application

 # 方式二: 打包后运行 fat JAR
 # java -jar blog-application/target/blog-application-*.jar
 ```

应用启动后，API 文档将自动在 `http://localhost:8080/swagger-ui.html` 上可用。

---

### 🤝 如何贡献

欢迎任何形式的贡献！如果你发现 Bug 或有任何改进建议，请随时：

1. Fork 本仓库
2. 创建你的功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交你的变更 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 发起一个 **Pull Request**

---

### 📜 许可证

本项目基于 [MIT License](LICENSE) 开源。

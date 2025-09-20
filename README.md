# 🚀 Personal Blog Backend

[![Java](https://img.shields.io/badge/Java-21-blue.svg?logo=openjdk)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5.5-brightgreen.svg?logo=spring)](https://spring.io/projects/spring-boot)
[![Maven](https://img.shields.io/badge/Maven-3.6+-orange.svg?logo=apache-maven)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/license-MIT-lightgrey.svg)](https://opensource.org/licenses/MIT)

这是一个基于 **Spring Boot 3 + Java 21** 构建的现代化、模块化的个人博客后端系统。项目采用多模块架构，旨在实现业务功能的高内聚、低耦合，使其易于维护和扩展。

---

### ✨ 核心功能

-   **文章管理 (Article Module)**: 提供文章的发布、编辑、删除和查询等全套 RESTful API。
-   **评论系统 (Comment Module)**: 支持对文章进行评论和回复。
-   **系统管理 (System Module)**: 包含用户认证、角色权限等基础管理功能。
-   **文件服务 (File Module)**: 提供统一的文件上传和管理能力。

---

### 🛠️ 技术栈

-   **核心框架**: Spring Boot 3.5.5
-   **开发语言**: Java 21
-   **构建工具**: Apache Maven
-   **代码简化**: Project Lombok
-   **对象映射**: MapStruct
-   **数据库**: (待定, 例如 MySQL, PostgreSQL)
-   **持久层**: (待定, 例如 Spring Data JPA, MyBatis-Plus)

---

### 🏗️ 项目架构

本项目采用 Maven 多模块设计，结构清晰，职责分明。

```
personal-blog-backend (根模块)
│
├── pom.xml                   # 根POM，统一管理依赖和插件版本
│
├── blog-application          # 🚀 应用启动模块
│   └── (包含 Spring Boot 启动类, application.yml 等)
│
├── blog-common               # 🔧 公共工具模块
│   └── (存放工具类, 全局常量, 通用 DTO, 异常处理等)
│
└── blog-modules              # 📦 核心业务模块 (父模块)
    │
    ├── blog-module-article   # 📄 文章模块
    ├── blog-module-comment   # 💬 评论模块
    ├── blog-module-system    # 🛡️ 系统管理模块
    └── blog-module-file      # 📁 文件管理模块

```

*   **根 POM (`personal-blog-backend`)**: 使用 `<dependencyManagement>` 和 `<pluginManagement>` 统一管理整个项目的依赖和插件版本，确保版本一致性。
*   **`blog-application`**: 项目的唯一入口，负责启动 Spring Boot 应用和组装所有业务模块。
*   **`blog-common`**: 存放被所有模块共享的代码，避免重复造轮子。
*   **`blog-modules`**: 这是一个父模块，不包含代码，仅用于聚合所有的业务功能模块，方便管理。每个子模块（如 `blog-module-article`）都遵循高内聚原则，独立负责一块业务领域。

---

### 🚀 快速开始

**环境要求**:
*   JDK 21 或更高版本
*   Maven 3.6+
*   一个正在运行的数据库实例 (例如 MySQL 8.0)

**步骤**:

1.  **克隆项目到本地**
    ```bash
    git clone https://github.com/liuxsml/personal-blog-backend.git
    cd personal-blog-backend
    ```

2.  **配置数据库**
    *   在 `blog-application` 模块的 `src/main/resources/` 目录下找到 `application.yml` (或 `.properties`) 文件。
    *   修改 `spring.datasource` 相关配置，指向你的数据库。

3.  **使用 Maven 构建项目**
    ```bash
    # 这会编译所有模块并安装到本地 Maven 仓库
    mvn clean install
    ```

4.  **运行应用**
    ```bash
    # 方式一: 直接通过 Spring Boot 插件运行
    mvn spring-boot:run -pl blog-application

    # 方式二: 打包后运行
    # mvn package -pl blog-application
    # java -jar blog-application/target/blog-application-*.jar
    ```

应用启动后，你可以通过 `http://localhost:8080` (或你配置的端口) 访问 API。

---

### 🤝 如何贡献

欢迎任何形式的贡献！如果你发现 Bug 或有任何改进建议，请随时：
1.  Fork 本仓库
2.  创建你的功能分支 (`git checkout -b feature/AmazingFeature`)
3.  提交你的变更 (`git commit -m 'Add some AmazingFeature'`)
4.  推送到分支 (`git push origin feature/AmazingFeature`)
5.  发起一个 **Pull Request**

---

### 📜 许可证

本项目基于 [MIT License](LICENSE) 开源。

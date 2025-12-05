# 🏗️ 个人博客后端架构设计指南 (Modular Monolith)

本文档旨在阐述 `personal-blog-backend` 项目的架构设计原则、模块划分标准以及开发注意事项。本项目采用 **模块化单体 (Modular Monolith)** 架构，旨在兼顾单体应用的开发便捷性与微服务架构的可扩展性。

---

## 1. 核心架构模式：模块化单体

我们选择“模块化单体”作为起步架构。这意味着：
*   **物理上**：所有代码打包在一个 JAR 包中，运行在一个 JVM 进程内（部署简单、性能高）。
*   **逻辑上**：严格遵循微服务的拆分原则，模块间高度隔离（为未来拆分留后路）。

### 为什么这样做？
*   避免了微服务初期带来的分布式事务、服务治理、运维复杂性等巨大的额外成本。
*   当某个业务模块（如文章模块）流量暴增时，可以轻松将其独立拆分为微服务，而无需重构代码。

---

## 2. 模块职责划分

我们将业务模块拆分为 `API` 和 `Service` 两部分，形象比喻为“餐厅菜单”与“后厨”。

### 2.1 `*-api` 模块 (菜单 📋)
**定位**：对外暴露的契约，**轻量、无依赖**。
**包含内容**：
*   **DTO (Data Transfer Object)**: 数据传输对象（如 `UserDTO`）。*绝对禁止包含数据库实体 (@Entity)*。
*   **Interfaces**: 暴露给其他模块调用的接口定义（如 `RemoteUserService`）。
*   **Enums/Constants**: 全局通用的枚举和常量。

### 2.2 `*-service` 模块 (后厨 👨‍🍳)
**定位**：核心业务实现，**私有、全功能**。
**包含内容**：
*   **Controller**: Web 层接口实现 (`@RestController`)。*注意：Controller 必须写在这里，而不是启动模块！*
*   **Service Impl**: 业务逻辑实现。
*   **Entity**: 数据库实体类（与数据库表一一对应）。
*   **Mapper/Repository**: 持久层接口。
*   **Config**: 模块私有的配置（如 Security 配置）。

### 2.3 `blog-application` 模块 (启动器 🚀)
**定位**：应用的组装与启动。
**职责**：
*   聚合所有 `*-service` 模块。
*   提供 `main` 方法启动 Spring Boot。
*   包含全局性的配置（如 `application.yml` 数据库配置）。
*   **严禁**：在此模块编写任何业务逻辑或 Controller。

---

## 3. 开发红线与注意事项 (Precautions) ⚠️

为了确保架构不腐化，以下规则必须严格遵守：

### 3.1 依赖原则 (The Dependency Rule)
*   ✅ **允许**：`Service` 模块依赖 `API` 模块（如 `article-service` -> `system-api`）。
*   ❌ **禁止**：`Service` 模块依赖 `Service` 模块（如 `article-service` -> `system-service`）。
    *   *后果*：一旦发生这种依赖，模块就无法独立拆分。

### 3.2 数据库隔离 (Data Isolation)
*   ❌ **禁止**：跨模块表 Join 查询（如 `SELECT * FROM article a JOIN user u ...`）。
*   ✅ **正确**：
    1.  文章模块查询文章数据。
    2.  通过 RPC 接口（本地调用）`userService.getByIds(...)` 获取用户数据。
    3.  在内存中（Java 代码）进行数据组装。
    *   *理由*：微服务架构下，数据库是物理隔离的，Join 根本无法执行。

### 3.3 接口即契约
*   模块间调用必须通过 `*-api` 中定义的 **Interface**，而不能直接依赖实现类。
*   Spring 会自动注入本地实现，未来切换为 Feign Client (远程调用) 时，业务代码无需修改。

### 3.4 避免“上帝类”
*   `blog-common` 仅用于存放真正的工具类（如 `DateUtils`, `Result`）。
*   不要将业务对象（如 `User` 实体）放入 `common`，这会导致所有模块都能随意修改它，破坏封装性。

---

## 4. 未来演进路线 (To Microservices)

当需要将 `blog-article` 拆分为独立微服务时，只需：
1.  创建一个新的启动模块 `blog-article-app`。
2.  引入 `blog-article-service` 依赖。
3.  配置 `application.yml` 连接独立的数据库。
4.  在其他模块中，将 `RemoteUserService` 的实现从本地 Bean 替换为 Feign Client。

---
*文档维护者: Antigravity Agent*
*最后更新: 2025-12-04*

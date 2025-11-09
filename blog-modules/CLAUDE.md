[根目录](../CLAUDE.md) > **blog-modules**

# blog-modules 模块

## 模块职责
所有业务领域模块（如系统、文章、评论、文件）的聚合器。此模块本身不包含业务逻辑，仅用于Maven项目的模块组织和管理。

## 模块构成
该模块聚合了以下子模块，每个子模块都代表一个独立的业务领域：
- `blog-module-system`: 系统管理模块，包含用户、权限等。
- `blog-module-article`: 文章管理模块，处理文章的CRUD操作。
- `blog-module-comment`: 评论管理模块，处理用户评论。
- `blog-module-file`: 文件管理模块，处理文件上传、下载等。

## 入口与启动
- 此模块是Maven的聚合模块，无启动类和可执行代码。
- 其主要作用是定义子模块之间的关系，并通过Maven的 `pom.xml` 管理它们的构建生命周期。

## 对外接口
- 无直接对外接口，其对外接口由其内部聚合的 `*-api` 子模块提供。

## 关键依赖与配置

### 主要依赖
- 此模块不包含具体的 `dependencies` 元素，所有依赖均由其子模块各自声明。
- 作为父POM，它可能定义 `dependencyManagement` 来统一管理子模块的依赖版本。

## 数据模型
- 此模块不包含直接的数据模型，数据模型定义在其业务子模块的 `*-api` 和 `*-service` 中。

## 测试与质量
- 无直接的测试，其子模块会进行各自的单元测试和集成测试。

## 常见问题 (FAQ)

### Q: 为什么需要一个 `blog-modules` 聚合模块？
A: 聚合模块有助于将相关的业务领域模块组织在一起，提供清晰的结构，并方便统一构建和管理。

### Q: 如何添加新的业务模块？
A: 在 `blog-modules` 目录下创建新的模块文件夹，编辑 `blog-modules/pom.xml` 文件，在 `<modules>` 标签中添加新的 `<module>` 条目。

## 相关文件清单
- `/Users/liusx/CodeRepository/IdeaProjects/personal-blog-backend/blog-modules/pom.xml` - Maven聚合配置

## 变更记录 (Changelog)

### 2025-11-09
- 架构师完成模块扫描，更新模块文档，明确其聚合职责和构成。

### 2025-09-19
- 创建模块文档
- 模块处于初始化状态，待完善子模块描述

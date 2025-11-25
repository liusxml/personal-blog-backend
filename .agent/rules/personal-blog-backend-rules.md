---
trigger: always_on
---

项目核心架构:
构建工具: Maven
整体结构: 采用模块化的单体架构 (Modular Monolith)，代码隔离清晰，职责分离。
主要模块:
personal-blog-backend: 根模块，统一管理依赖和插件。
blog-common: 通用工具、常量、基础实体和异常。
blog-modules: 聚合所有业务领域模块 (system, article, comment, file)。
blog-application: Spring Boot 启动模块，负责组装和运行整个应用。
blog-admin-server: 独立的 Spring Boot Admin 监控服务端。 核心技术栈:
语言和框架: Java 21, Spring Boot 3.5.7
持久层: MyBatis-Plus 3.5.14
数据库: MySQL (Connector Version 9.4.0)
安全: Spring Security
API 文档: SpringDoc OpenAPI (Swagger 3)
开发工具:
Lombok 1.18.42
MapStruct 1.6.3 用于 DTO 转换，并已配置 lombok-mapstruct-binding 增强兼容性。
常用工具库:
commons-lang3 3.18.0
commons-collections4 4.5.0
commons-io 2.20.0
guava 33.5.0-jre 

使用context7
[根目录](../CLAUDE.md) > **blog-application**

# blog-application 模块

## 模块职责
主应用启动模块，负责Spring Boot应用的启动、全局配置管理和依赖注入。

## 入口与启动

### 主启动类
```java
package com.blog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BlogApplication {
    public static void main(String[] args) {
        SpringApplication.run(BlogApplication.class, args);
    }
}
```

### 配置文件
- `/Users/liusx/CodeRepository/IdeaProjects/personal-blog-backend/blog-application/src/main/resources/application.yaml`: 应用配置文件
  - 服务器端口：8080 (默认)
  - 数据库连接配置
  - MyBatis-Plus配置
  - SpringDoc OpenAPI配置
  - Spring Security 配置
  - Redis 配置
- `/Users/liusx/CodeRepository/IdeaProjects/personal-blog-backend/blog-application/src/main/resources/logback-spring.xml`: Logback日志配置文件

## 对外接口
- 提供RESTful API接口
- Swagger UI文档：http://localhost:8080/swagger-ui.html
- OpenAPI文档：http://localhost:8080/v3/api-docs
- Actuator 端点：默认保护，`/health` 和 `/info` 等公共端点可能配置放行。

## 关键依赖与配置

### 主要依赖
- Spring Boot Starter Web: Web应用核心。
- Spring Boot Starter Validation: 参数校验。
- Spring Boot Starter Security: 安全框架。
- SpringDoc OpenAPI Starter Webmvc UI: API文档生成。
- MyBatis-Plus Spring Boot3 Starter: MyBatis-Plus集成。
- Spring Boot Starter Actuator: 应用监控。
- Spring Boot Admin Starter Client: Spring Boot Admin 客户端。
- Spring Boot Starter Data Redis: Redis数据访问。
- MySQL Connector/J: MySQL数据库驱动。
- MyBatis-Plus Jsqlparser: MyBatis-Plus SQL解析器。
- 项目内部模块: `blog-system-service`, `blog-article-service`, `blog-comment-service`, `blog-file-service`。
- 开发与测试工具: `spring-boot-devtools`, `spring-boot-starter-test`, `h2database`。

### 数据库配置 (示例)
```yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/personal_blog?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: your_password
    driver-class-name: com.mysql.cj.jdbc.Driver
```

## 数据模型
- 通过MyBatis-Plus进行数据访问
- 实体类分布在各个业务模块中
- 数据库初始化脚本: `/Users/liusx/CodeRepository/IdeaProjects/personal-blog-backend/blog-application/src/main/resources/db/v1.0.0_init_schema.sql`

## 测试与质量
- 单元测试：JUnit 5
- 集成测试：Spring Boot Test (例如 `SecurityIntegrationTest.java`, `RedisUtilsTest.java`)
- 代码质量：待配置检查工具

## 常见问题 (FAQ)

### Q: 如何修改数据库连接？
A: 编辑 `/Users/liusx/CodeRepository/IdeaProjects/personal-blog-backend/blog-application/src/main/resources/application.yaml` 文件中的数据库配置部分

### Q: 如何访问API文档？
A: 启动应用后访问 http://localhost:8080/swagger-ui.html

## 相关文件清单
- `/Users/liusx/CodeRepository/IdeaProjects/personal-blog-backend/blog-application/pom.xml` - Maven配置
- `/Users/liusx/CodeRepository/IdeaProjects/personal-blog-backend/blog-application/src/main/java/com/blog/BlogApplication.java` - 主启动类
- `/Users/liusx/CodeRepository/IdeaProjects/personal-blog-backend/blog-application/src/main/resources/application.yaml` - 应用配置文件
- `/Users/liusx/CodeRepository/IdeaProjects/personal-blog-backend/blog-application/src/main/resources/logback-spring.xml` - Logback日志配置文件
- `/Users/liusx/CodeRepository/IdeaProjects/personal-blog-backend/blog-application/src/main/resources/db/v1.0.0_init_schema.sql` - 数据库初始化脚本
- `/Users/liusx/CodeRepository/IdeaProjects/personal-blog-backend/blog-application/src/main/java/com/blog/config/MybatisPlusConfig.java` - MyBatis-Plus 配置
- `/Users/liusx/CodeRepository/IdeaProjects/personal-blog-backend/blog-application/src/main/java/com/blog/config/MybatisPlusHandlerConfig.java` - MyBatis-Plus 处理器配置
- `/Users/liusx/CodeRepository/IdeaProjects/personal-blog-backend/blog-application/src/main/java/com/blog/handler/MyMetaObjectHandler.java` - MyBatis-Plus 元对象处理器
- `/Users/liusx/CodeRepository/IdeaProjects/personal-blog-backend/blog-application/src/main/java/com/blog/config/RedisConfig.java` - Redis 配置
- `/Users/liusx/CodeRepository/IdeaProjects/personal-blog-backend/blog-application/src/main/java/com/blog/config/SecurityConfig.java` - Spring Security 配置
- `/Users/liusx/CodeRepository/IdeaProjects/personal-blog-backend/blog-application/src/main/java/com/blog/config/SecurityProperties.java` - Spring Security 配置属性
- `/Users/liusx/CodeRepository/IdeaProjects/personal-blog-backend/blog-application/src/main/java/com/blog/handler/GlobalExceptionHandler.java` - 全局异常处理器
- `/Users/liusx/CodeRepository/IdeaProjects/personal-blog-backend/blog-application/src/main/java/com/blog/config/ddl/DdlInitializer.java` - DDL 初始化器
- `/Users/liusx/CodeRepository/IdeaProjects/personal-blog-backend/blog-application/src/main/java/com/blog/config/ddl/MybatisPlusMysqlDdlManager.java` - MyBatis-Plus MySQL DDL 管理器
- `/Users/liusx/CodeRepository/IdeaProjects/personal-blog-backend/blog-application/src/test/java/com/blog/frameworktest/converter/TestConverter.java` - 测试转换器
- `/Users/liusx/CodeRepository/IdeaProjects/personal-blog-backend/blog-application/src/test/java/com/blog/frameworktest/service/ITestService.java` - 测试服务接口
- `/Users/liusx/CodeRepository/IdeaProjects/personal-blog-backend/blog-application/src/test/resources/mapper/TestMapper.xml` - 测试 Mapper XML
- `/Users/liusx/CodeRepository/IdeaProjects/personal-blog-backend/blog-application/src/test/java/com/blog/frameworktest/service/impl/TestServiceImpl.java` - 测试服务实现
- `/Users/liusx/CodeRepository/IdeaProjects/personal-blog-backend/blog-application/src/test/java/com/blog/frameworktest/mapper/TestMapper.java` - 测试 Mapper 接口
- `/Users/liusx/CodeRepository/IdeaProjects/personal-blog-backend/blog-application/src/test/java/com/blog/frameworktest/vo/TestVO.java` - 测试 VO
- `/Users/liusx/CodeRepository/IdeaProjects/personal-blog-backend/blog-application/src/test/java/com/blog/frameworktest/dto/ValidationTestDTO.java` - 验证测试 DTO
- `/Users/liusx/CodeRepository/IdeaProjects/personal-blog-backend/blog-application/src/test/java/com/blog/frameworktest/vo/NullMessage.java` - 空消息注解
- `/Users/liusx/CodeRepository/IdeaProjects/personal-blog-backend/blog-application/src/test/java/com/blog/frameworktest/vo/NullMessageValidator.java` - 空消息验证器

## 变更记录 (Changelog)

### 2025-11-09
- 架构师完成模块扫描，更新模块文档，补充关键依赖和相关文件清单，包含详细的配置和测试文件路径。

### 2025-09-19
- 创建模块文档
- 记录启动类和配置信息

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
- `src/main/resources/application.yaml`: 应用配置文件
  - 服务器端口：8080
  - 数据库连接配置
  - MyBatis-Plus配置
  - SpringDoc OpenAPI配置

## 对外接口
- 提供RESTful API接口
- Swagger UI文档：http://localhost:8080/swagger-ui.html
- OpenAPI文档：http://localhost:8080/v3/api-docs

## 关键依赖与配置

### 主要依赖
- Spring Boot 3.2.5
- Spring Web MVC
- MyBatis-Plus 3.5.6
- MySQL Connector 8.2.0
- SpringDoc OpenAPI 2.5.0

### 数据库配置
```yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/personal_blog
    username: root
    password: s3cr3t_r00t_p@ssw0rd
    driver-class-name: com.mysql.cj.jdbc.Driver
```

## 数据模型
- 通过MyBatis-Plus进行数据访问
- 实体类分布在各个业务模块中

## 测试与质量
- 单元测试：JUnit 5
- 集成测试：Spring Boot Test
- 代码质量：待配置检查工具

## 常见问题 (FAQ)

### Q: 如何修改数据库连接？
A: 编辑 `application.yaml` 文件中的数据库配置部分

### Q: 如何访问API文档？
A: 启动应用后访问 http://localhost:8080/swagger-ui.html

## 相关文件清单
- `src/main/java/com/blog/BlogApplication.java` - 主启动类
- `src/main/resources/application.yaml` - 应用配置文件
- `pom.xml` - Maven配置

## 变更记录 (Changelog)

### 2025-09-19
- 创建模块文档
- 记录启动类和配置信息
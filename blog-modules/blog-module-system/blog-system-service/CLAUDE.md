[根目录](../../../CLAUDE.md) > [blog-modules](../) > [blog-module-system](../) > **blog-system-service**

# blog-system-service 模块

## 模块职责
系统模块业务逻辑实现，负责用户管理相关的业务逻辑、数据持久化、事务管理等服务层实现。

## 入口与启动
- 此模块为服务实现模块，不包含启动类
- 实现blog-system-api模块定义的接口
- 需要配置数据源和事务管理

## 对外接口
- 实现UserApi接口定义的所有方法
- 提供用户相关的业务服务
- 暴露服务接口供控制器调用

## 关键依赖与配置

### 主要依赖
- MyBatis-Plus 3.5.6 - 数据访问
- Spring Data JDBC/Transaction - 事务管理
- 数据库连接池（HikariCP）

### 数据源配置
- 继承根pom的数据库配置
- 需要配置Mapper扫描路径
- 需要配置事务管理器

## 数据模型
- 用户实体类（待创建）
- 用户角色实体类（待创建）
- 权限实体类（待创建）
- MyBatis-Plus Mapper接口

## 测试与质量
- 服务层单元测试
- 数据库集成测试
- 事务管理测试

## 常见问题 (FAQ)

### Q: 如何配置数据源？
A: 继承根pom的配置，或在application.yaml中添加特定配置

### Q: 如何处理事务？
A: 使用@Transactional注解管理事务边界

## 相关文件清单
- `pom.xml` - Maven配置
- 服务实现类（待创建）
- 数据访问层文件（待创建）
- 实体类文件（待创建）

## 变更记录 (Changelog)

### 2025-09-19
- 创建模块文档
- 模块处于初始化状态，业务逻辑待实现
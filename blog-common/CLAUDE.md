[根目录](../../CLAUDE.md) > **blog-common**

# blog-common 模块

## 模块职责
公共工具模块，提供项目中通用的工具类、常量定义、异常处理、响应格式等共享组件。

## 入口与启动
- 此模块为工具类模块，不包含启动类
- 被其他业务模块依赖使用

## 对外接口
- 提供静态工具方法
- 定义通用DTO和VO基类
- 统一异常处理机制
- 标准响应格式定义

## 关键依赖与配置

### 主要依赖
- Spring Context: 提供核心IoC功能和基础注解。
- MyBatis-Plus: 核心类用于定义BaseService，提供IService, Wrapper, BaseMapper等。
- Lombok: 编译期自动生成代码，减少样板代码。
- Apache Commons Lang3/Collections4: 常用工具库，提供字符串、集合等操作。
- Guava: Google核心库，提供集合、缓存、字符串处理等。
- Spring Transaction: 提供事务管理能力。
- MapStruct: 对象映射工具。
- Spring Data Redis: 提供Redis数据访问支持。

## 数据模型
- 通用数据转换对象（DTO）基类
- 通用视图对象（VO）基类
- 分页查询参数类
- 统一响应封装类

## 测试与质量
- 工具类单元测试
- 通用组件功能测试

## 常见问题 (FAQ)

### Q: 如何添加新的通用工具类？
A: 在适当的包结构下创建工具类，确保方法为静态且线程安全

### Q: 如何统一异常处理？
A: 创建全局异常处理器，定义业务异常枚举

## 相关文件清单
- `/Users/liusx/CodeRepository/IdeaProjects/personal-blog-backend/blog-common/pom.xml` - Maven配置
- `/Users/liusx/CodeRepository/IdeaProjects/personal-blog-backend/blog-common/src/main/java/com/blog/common/exception/BusinessException.java` - 业务异常定义
- `/Users/liusx/CodeRepository/IdeaProjects/personal-blog-backend/blog-common/src/main/java/com/blog/common/exception/ErrorCode.java` - 错误码枚举
- `/Users/liusx/CodeRepository/IdeaProjects/personal-blog-backend/blog-common/src/main/java/com/blog/common/model/Result.java` - 统一响应封装类

## 变更记录 (Changelog)

### 2025-11-09
- 架构师完成模块扫描，更新模块文档，补充关键依赖和相关文件清单。

### 2025-09-19
- 创建模块文档
- 模块处于初始化状态，待完善工具类

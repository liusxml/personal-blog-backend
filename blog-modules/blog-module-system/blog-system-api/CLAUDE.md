[根目录](../../../CLAUDE.md) > [blog-modules](../) > [blog-module-system](../) > **blog-system-api**

# blog-system-api 模块

## 模块职责
系统模块API接口定义，负责用户管理相关的RESTful接口定义和DTO/VO对象定义。

## 入口与启动
- 此模块为API定义模块，不包含启动类
- 需要被blog-system-service模块实现
- 被blog-application模块依赖

## 对外接口

### 用户管理接口
- 用户注册接口
- 用户登录接口
- 用户信息查询接口
- 用户信息修改接口
- 用户权限管理接口

### 接口定义文件
- `UserApi.java` - 用户相关API接口定义（待完善）

## 关键依赖与配置

### 主要依赖
- Spring Web注解
- 验证注解（Jakarta Validation）
- Swagger/OpenAPI注解

## 数据模型

### DTO对象
- `UserDTO.java` - 用户数据传输对象（待完善）

### VO对象
- `UserInfoVO.java` - 用户信息视图对象（待完善）

## 测试与质量
- API接口契约测试
- DTO/VO对象验证测试
- Swagger文档生成验证

## 常见问题 (FAQ)

### Q: 如何添加新的API接口？
A: 创建新的接口类，使用Spring Web注解定义REST端点

### Q: 如何定义请求参数验证？
A: 在DTO对象中使用Jakarta Validation注解

## 相关文件清单
- `src/main/java/com/blog/UserApi.java` - 用户API接口
- `src/main/java/com/blog/model/dto/UserDTO.java` - 用户DTO
- `src/main/java/com/blog/model/vo/UserInfoVO.java` - 用户信息VO
- `pom.xml` - Maven配置

## 变更记录 (Changelog)

### 2025-09-19
- 创建模块文档
- 模块处于初始化状态，接口和模型类待完善
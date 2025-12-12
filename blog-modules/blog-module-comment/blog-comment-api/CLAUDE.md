[根目录](../../../CLAUDE.md) > [blog-modules](../) > [blog-module-comment](../) > **blog-comment-api**

# blog-comment-api 模块

## 模块职责
评论模块API接口定义，负责评论管理相关的RESTful接口定义和DTO/VO对象定义。

## 入口与启动
- 此模块为API定义模块，不包含启动类
- 需要被blog-comment-service模块实现
- 被blog-application模块依赖

## 对外接口

### 评论管理接口
- 评论创建接口
- 评论查询接口
- 评论删除接口
- 评论列表查询接口
- 评论回复接口
- 评论点赞接口

### 接口定义文件
- 评论相关API接口定义（待创建）

## 关键依赖与配置

### 主要依赖
- Spring Web注解
- 验证注解（Jakarta Validation）
- Swagger/OpenAPI注解

## 数据模型

### DTO对象
- 评论创建DTO（待创建）
- 评论回复DTO（待创建）

### VO对象
- 评论详情VO（待创建）
- 评论列表VO（待创建）

## 测试与质量
- API接口契约测试
- DTO/VO对象验证测试
- Swagger文档生成验证

## 常见问题 (FAQ)

### Q: 如何设计评论层级结构？
A: 使用parent_id字段实现多级评论，支持回复功能

### Q: 如何处理评论审核？
A: 设计评论状态枚举，包含待审核、已通过、已拒绝等状态

## 相关文件清单
- `pom.xml` - Maven配置
- API接口类（待创建）
- DTO/VO对象类（待创建）

## 变更记录 (Changelog)

### 2025-09-19
- 创建模块文档
- 模块处于初始化状态，待创建接口和模型类
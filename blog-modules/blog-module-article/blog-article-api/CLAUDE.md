[根目录](../../../CLAUDE.md) > [blog-modules](../) > [blog-module-article](../) > **blog-article-api**

# blog-article-api 模块

## 模块职责
文章模块API接口定义，负责文章管理相关的RESTful接口定义和DTO/VO对象定义。

## 入口与启动
- 此模块为API定义模块，不包含启动类
- 需要被blog-article-service模块实现
- 被blog-application模块依赖

## 对外接口

### 文章管理接口
- 文章创建接口
- 文章查询接口
- 文章修改接口
- 文章删除接口
- 文章列表查询接口
- 文章详情查询接口

### 接口定义文件
- 文章相关API接口定义（待创建）

## 关键依赖与配置

### 主要依赖
- Spring Web注解
- 验证注解（Jakarta Validation）
- Swagger/OpenAPI注解

## 数据模型

### DTO对象
- `ArticleCreateDTO.java` - 文章创建DTO（已创建，待完善）

### VO对象
- `ArticleDetailVO.java` - 文章详情视图对象（已创建，待完善）

## 测试与质量
- API接口契约测试
- DTO/VO对象验证测试
- Swagger文档生成验证

## 常见问题 (FAQ)

### Q: 如何设计文章状态枚举？
A: 创建文章状态枚举类，包含草稿、已发布、已删除等状态

### Q: 如何处理文章分类？
A: 可以设计分类DTO和分类管理接口

## 相关文件清单
- `src/main/java/com/blog/model/dot/ArticleCreateDTO.java` - 文章创建DTO
- `src/main/java/com/blog/model/vo/ArticleDetailVO.java` - 文章详情VO
- `pom.xml` - Maven配置
- API接口类（待创建）

## 变更记录 (Changelog)

### 2025-09-19
- 创建模块文档
- 模块处于初始化状态，已有部分DTO/VO文件
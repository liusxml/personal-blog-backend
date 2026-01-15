# 代码优化实施说明

根据用户确认，`sys_user_role` 表属于系统模块，这是架构设计的一部分。因此 `UserMapper.selectRolesByUserId()` 中的 JOIN
查询是合理的。

## 表归属说明

### 系统模块核心表

**blog-module-system** 包含以下核心表：

1. **sys_user** - 用户表
2. **sys_role** - 角色表
3. **sys_user_role** - 用户角色关联表（多对多中间表）

### 设计原则

- `sys_user_role` 作为**关联表**存在于系统模块内部
- 这三张表构成了完整的**用户权限管理域**
- 在同一模块内进行 JOIN 查询是**符合微服务就绪原则**的
- 未来拆分为微服务时，这三张表将作为一个整体迁移到**用户服务**

### 微服务拆分策略

当需要拆分为微服务架构时：

```
用户服务 (User Service):
  └─ sys_user
  └─ sys_role  
  └─ sys_user_role
```

所有用户-角色相关操作将由**用户服务**统一提供 API，其他服务通过 **RemoteUserService** 接口调用。

### SQL 查询合规性

✅ **允许的查询**（同模块内）:

```sql
-- 用户-角色关联查询（sys_user, sys_role, sys_user_role 都在 blog-module-system）
SELECT r.* FROM sys_role r
INNER JOIN sys_user_role ur ON r.id = ur.role_id  
WHERE ur.user_id = ?
```

❌ **禁止的查询**（跨业务模块）:

```sql
-- 假设 art_article 在 blog-module-article 模块
SELECT *
FROM sys_user u
         INNER JOIN art_article a ON u.id = a.author_id -- 违反模块边界
```

## 架构决策记录 (ADR)

**决策**: 将用户、角色、用户角色关联表放在同一个业务模块（blog-module-system）

**理由**:

1. 这三张表构成了**内聚的业务域**（RBAC 权限模型）
2. 用户角色是用户的**固有属性**，不应分离
3. 避免过度设计，保持**适度的模块粒度**

**影响**:

- 同模块内可以使用 JOIN 查询，性能更优
- 未来迁移微服务时，这三张表作为整体迁移，无需重构
- 跨模块访问用户数据通过 `RemoteUserService` 接口

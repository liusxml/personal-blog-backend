# Git 提交规范指南 (Conventional Commits)

本项目遵循业界标准的 **Conventional Commits** 规范。使用标准化的提交信息有助于自动化生成更新日志 (Changelog)、语义化版本控制以及提高团队协作效率。

## 1. 提交格式

一条标准的 Git 提交信息由 **Header**, **Body**, **Footer** 组成：

```text
<type>(<scope>): <subject>
// 空一行
<body>
// 空一行
<footer>
```

- **type**: 提交类型（必填，见下文）。
- **scope**: 影响范围（选填，如 `auth`, `user`, `pom`）。
- **subject**: 简短描述（必填，不超过 50 字）。
- **body**: 详细描述（选填，解释“为什么”修改，而不是“怎么”修改）。
- **footer**: 备注信息（选填，如关联 Issue 或 Breaking Changes）。

---

## 2. Type 详细说明 (核心)

以下是必须掌握的核心类型：

### ✨ feat (Feature)
- **含义**：新增功能。
- **场景**：开发了一个新接口、新页面或新组件。
- **示例**：`feat(user): 新增用户注册接口`

### 🐛 fix (Bug Fix)
- **含义**：修复 Bug。
- **场景**：修复了空指针异常、逻辑错误或样式问题。
- **示例**：`fix(auth): 修复 token 过期时间计算错误`

### 📚 docs (Documentation)
- **含义**：文档变更。
- **场景**：更新 README、API 文档或注释，不包含代码修改。
- **示例**：`docs(readme): 更新项目启动说明`

### 💎 style (Styles)
- **含义**：代码格式调整（**注意不是 CSS 样式**）。
- **场景**：空格、分号、换行等不影响代码运行逻辑的修改。
- **示例**：`style(user): 格式化 UserServiceImpl 代码`

### ♻️ refactor (Refactoring)
- **含义**：代码重构。
- **场景**：既不新增功能也不修复 Bug 的代码修改（如提取方法、重命名变量）。
- **示例**：`refactor(common): 重构 Result 响应类结构`

### 🚀 perf (Performance)
- **含义**：性能优化。
- **场景**：提升代码执行速度或减少内存占用的修改。
- **示例**：`perf(db): 优化用户查询 SQL 索引`

### 🧪 test (Tests)
- **含义**：测试相关。
- **场景**：添加或修改单元测试，不影响生产代码。
- **示例**：`test(auth): 添加登录接口单元测试`

### 🔧 chore (Chores)
- **含义**：杂项/构建/工具。
- **场景**：修改 .gitignore、pom.xml 依赖更新、构建脚本等。
- **示例**：`chore(deps): 升级 spring-boot 版本到 3.2.0`

### ⏪ revert (Revert)
- **含义**：回退代码。
- **场景**：撤销之前的某次提交。
- **示例**：`revert: 撤销 "feat(user): 新增删除用户功能"`

---

## 3. 完整示例

### 简单示例
```git
feat(blog): 新增文章发布功能
```

### 完整示例
```git
fix(security): 修复 Swagger UI 登录拦截问题

目前 Swagger UI 在未登录时会被拦截重定向，
导致无法查看文档。

解决方法：
1. 将 Swagger 相关路径加入 permit-all-urls 白名单。
2. 调整 SecurityConfig 中的过滤链顺序。

Closes #123
```

## 4. 为什么要这样做？

1.  **自动生成 Changelog**：工具可以直接提取 `feat` 和 `fix` 生成发布日志。
2.  **语义化版本**：`fix` 对应 patch 版本号，`feat` 对应 minor 版本号，`BREAKING CHANGE` 对应 major 版本号。
3.  **快速定位**：通过 `git log | grep fix` 可以快速找到所有的 Bug 修复记录。

# 📦 /docs 目录文档说明

此目录包含项目的原始文档文件。这些文档已被整理并迁移到 Docusaurus 文档站点。

## 📂 目录结构

### `/docs/docs/` - Docusaurus 文档站点
完整的文档站点，包含所有已迁移和整理的文档。

**访问方式**：
```bash
cd docs/docs
npm start  # 开发服务器
npm run build  # 生产构建
```

### `/docs/archive/` - 已归档文件
临时文件和内部文档的归档目录。详见 `archive/README.md`。

## 📊 文档迁移状态

### ✅ 已迁移到 Docusaurus（11个文件）

#### 测试文档
- ✅ `TESTING_GUIDE.md` → `docs/testing/overview.md`
- ✅ `ARCHUNIT_GUIDE.md` → `docs/testing/archunit.md`
- ✅ `MockBean-Deprecation-Guide.md` → `docs/testing/mockbean-migration.md`

#### 开发指南
- ✅ `development-standards.md` → `docs/development/standards.md`
- ✅ `BASE_FRAMEWORK_GUIDE.md` → `docs/development/base-framework.md`
- ✅ `GIT_COMMIT_GUIDE.md` → `docs/development/git-commit.md`

#### 基础设施
- ✅ `redis-usage-guide.md` → `docs/infrastructure/redis/overview.md`
- ✅ `CACHE_STRATEGY.md` → `docs/infrastructure/redis/cache-strategy.md`
- ✅ `SECURITY_GUIDE.md` → `docs/infrastructure/security/overview.md`
- ✅ `SPRINGDOC_GUIDE.md` → `docs/infrastructure/api-docs.md`
- ✅ `resilience4j-guide.md` → `docs/infrastructure/resilience4j.md`

#### 参考资料
- ✅ `knowledge-base.md` → `docs/reference/knowledge-base.md`

#### 业务模块
- ✅ `blog-module-file-analysis.md` → `docs/modules/file/analysis.md`

#### 架构设计
- ✅ `ARCHITECTURE_DESIGN.md` → `docs/architecture/overview.md` (已有新版本)
- ✅ `project-evaluation.md` → `docs/architecture/analysis.md`

### 📌 保留在原位置（参考用）

以下文件保留在此目录作为原始参考，已复制到 Docusaurus：
- `TESTING_GUIDE.md`
- `ARCHUNIT_GUIDE.md`
- `BASE_FRAMEWORK_GUIDE.md`
- `GIT_COMMIT_GUIDE.md`
- `redis-usage-guide.md`
- `CACHE_STRATEGY.md`
- `SECURITY_GUIDE.md`
- `SPRINGDOC_GUIDE.md`
- `development-standards.md`
- `resilience4j-guide.md`
- `knowledge-base.md`
- `blog-module-file-analysis.md`
- `MockBean-Deprecation-Guide.md`
- `project-evaluation.md`
- `intro.md`
- `quick-start.md`
- `CACHE_API_TEST_GUIDE.md` (未迁移)
- `ARCHITECTURE_DESIGN.md`

### 🗂️ 已归档（4个文件）
- `AGENT_RULES.md` - AI Agent 规则（内部文档）
- `test_report.md` - 临时测试报告
- `CACHE_API_COMPLETE_TEST_REPORT.md` - 临时测试报告
- `walkthrough_cn.md` - 临时开发记录

## 🎯 下一步建议

1. **查看文档站点**
   ```bash
   cd docs/docs
   npm start
   ```
   访问：http://localhost:3000/personal-blog-backend/

2. **后续维护**
   - 新文档应直接创建在 `docs/docs/docs/` 目录下
   - 遵循现有的分类结构
   - 记得添加 frontmatter 和更新 sidebars.ts

3. **可选清理**
   - 如果确认不再需要原始文件，可以考虑删除已迁移的文档
   - 建议保留一段时间以确保迁移完整无误

---

**整理日期**：2025-12-10  
**文档总数**：11 个已迁移 + 4 个已归档  
**Docusaurus 版本**：3.9.2

import type { SidebarsConfig } from '@docusaurus/plugin-content-docs';

/**
 * Personal Blog Backend 文档侧边栏配置
 * 
 * 结构说明：
 * - 按照学习路径组织：快速上手 -> 架构设计 -> 业务模块 -> 开发指南 -> 测试 -> 基础设施 -> 故障排查 -> API
 * - 使用 emoji 图标增强视觉效果
 * - 快速上手默认展开，其他分类默认折叠
 */
const sidebars: SidebarsConfig = {
  // 主文档侧边栏
  tutorialSidebar: [
    // ========== 1. 快速上手 ==========
    {
      type: 'category',
      label: '🚀 快速上手',
      collapsed: false,  // 默认展开，方便新用户
      link: {
        type: 'generated-index',
        title: '快速上手',
        description: '快速了解项目并开始使用，15分钟即可完成环境搭建。',
      },
      items: [
        'getting-started/intro',        // 项目介绍
        'getting-started/quick-start',  // 快速启动
      ],
    },

    // ========== 2. 架构设计 ==========
    {
      type: 'category',
      label: '🏗️ 架构设计',
      link: {
        type: 'generated-index',
        title: '架构设计',
        description: '深入理解模块化单体架构和设计原则。',
      },
      items: [
        'architecture/overview',  // 架构总览
        'architecture/analysis',  // 项目评价报告
      ],
    },

    // ========== 3. 业务模块 ==========
    {
      type: 'category',
      label: '📦 业务模块',
      link: {
        type: 'generated-index',
        title: '业务模块',
        description: '系统、文章、评论、文件四大业务模块详解。',
      },
      items: [
        {
          type: 'category',
          label: '🛡️ 系统模块',
          items: ['modules/system/intro'],
        },
        {
          type: 'category',
          label: '📄 文章模块',
          items: ['modules/article/intro'],
        },
        {
          type: 'category',
          label: '💬 评论模块',
          items: ['modules/comment/intro'],
        },
        {
          type: 'category',
          label: '📁 文件模块',
          items: ['modules/file/intro'],
        },
      ],
    },

    // ========== 4. 开发指南 ==========
    {
      type: 'category',
      label: '🛠️ 开发指南',
      link: {
        type: 'generated-index',
        title: '开发指南',
        description: '开发规范、Base 框架使用和工作流程。',
      },
      items: [
        'development/standards',        // 开发规范
        'development/base-framework',   // Base Framework 使用指南
        'development/git-commit',       // Git 提交规范
      ],
    },

    // ========== 5. 测试 ==========
    {
      type: 'category',
      label: '🧪 测试',
      link: {
        type: 'generated-index',
        title: '测试指南',
        description: '单元测试、集成测试和架构测试完整指南。',
      },
      items: [
        'testing/overview',           // 测试总览
        'testing/archunit',           // ArchUnit 架构测试
        'testing/mockbean-migration', // MockBean 迁移指南
      ],
    },

    // ========== 6. 基础设施 ==========
    {
      type: 'category',
      label: '🔧 基础设施',
      link: {
        type: 'generated-index',
        title: '基础设施',
        description: 'Redis、Security、数据库等基础设施配置和使用。',
      },
      items: [
        {
          type: 'category',
          label: 'Redis 缓存',
          items: [
            'infrastructure/redis/overview',        // Redis 使用指南
            'infrastructure/redis/cache-strategy',  // 缓存策略详解
            'infrastructure/redis/cache-api-guide', // 缓存 API 测试指南
          ],
        },
        {
          type: 'category',
          label: 'Security 安全',
          items: ['infrastructure/security/overview'],
        },
        'infrastructure/api-docs',        // SpringDoc API 文档
        'infrastructure/resilience4j',    // Resilience4j 容错指南
      ],
    },

    // ========== 7. 故障排查 ==========
    {
      type: 'category',
      label: '🔍 故障排查',
      link: {
        type: 'generated-index',
        title: '故障排查',
        description: '常见问题和错误处理方案。',
      },
      items: [
        'troubleshooting/ddl-transaction-pitfall',  // DDL 事务陷阱
      ],
    },

    // ========== 8. API 参考 ==========
    {
      type: 'category',
      label: '📚 API 参考',
      link: {
        type: 'generated-index',
        title: 'API 参考',
        description: '完整的 REST API 接口文档和使用示例。',
      },
      items: [
        'api/overview',  // API 概览
      ],
    },

    // ========== 9. 参考资料 ==========
    {
      type: 'category',
      label: '📚 参考资料',
      link: {
        type: 'generated-index',
        title: '参考资料',
        description: '项目知识库和快速参考手册。',
      },
      items: [
        'reference/knowledge-base',  // 项目知识库
      ],
    },
  ],
};

export default sidebars;

import { themes as prismThemes } from 'prism-react-renderer';
import type { Config } from '@docusaurus/types';
import type * as Preset from '@docusaurus/preset-classic';

const config: Config = {
    // --- 1. 核心站点信息 ---
    title: '个人博客后台系统',
    tagline: '一个高性能、易扩展的个人博客后端系统', // 直接来自你的项目描述，非常棒！
    favicon: 'img/logo.ico', // 建议你设计一个 Logo

    // --- 2. 部署与 GitHub 集成 (关键配置) ---
    url: 'https://liusxml.github.io', // 你的 GitHub Pages 域名
    baseUrl: '/personal-blog-backend/', // 你的仓库名称
    organizationName: 'liusxml', // 你的 GitHub 用户名
    projectName: 'personal-blog-backend', // 你的 GitHub 仓库名

    onBrokenLinks: 'throw',
    markdown: {
        hooks: {
            onBrokenMarkdownLinks: 'warn',
        },
    },

    // --- 3. 国际化配置 ---
    i18n: {
        defaultLocale: 'zh-Hans', // 设置为简体中文
        locales: ['zh-Hans'],
    },

    presets: [
        [
            'classic',
            {
                docs: {
                    sidebarPath: './sidebars.ts',
                    // --- 4. 内容编辑链接 (关键配置) ---
                    // 假设你的 Docusaurus 站点位于仓库根目录的 'docs' 文件夹下
                    editUrl:
                        'https://github.com/liusxml/personal-blog-backend/tree/main/docs/',
                },
                blog: {
                    showReadingTime: true,
                    // --- 同样，修改博客的编辑链接 ---
                    editUrl:
                        'https://github.com/liusxml/personal-blog-backend/tree/main/docs/',
                },
                theme: {
                    customCss: './src/css/custom.css',
                },
            } satisfies Preset.Options,
        ],
    ],

    themeConfig: {
        image: 'img/social-card.png', // 建议你创建一个项目社交预览图

        // --- 5. 导航栏定制 (核心体验) ---
        navbar: {
            title: '个人博客后台系统',
            logo: {
                alt: '项目 Logo',
                src: 'img/logo.svg',
            },
            items: [
                {
                    type: 'docSidebar',
                    sidebarId: 'tutorialSidebar',
                    position: 'left',
                    label: '开发指南', // 例如：快速上手、架构设计等
                },
                // 新增一个下拉菜单，专门用于展示各个核心模块的文档
                {
                    type: 'dropdown',
                    label: '模块详解',
                    position: 'left',
                    items: [
                        {
                            label: '系统模块 (System)',
                            to: '/docs/modules/system/intro', // 链接到系统模块的介绍页
                        },
                        {
                            label: '文章模块 (Article)',
                            to: '/docs/modules/article/intro', // 链接到文章模块的介绍页
                        },
                        {
                            label: '评论模块 (Comment)',
                            to: '/docs/modules/comment/intro', // 链接到评论模块的介绍页
                        },
                        {
                            label: '文件模块 (File)',
                            to: '/docs/modules/file/intro', // 链接到文件模块的介绍页
                        },
                    ],
                },
                {
                    to: '/docs/api/overview', // 专门的 API 参考入口
                    label: 'API 参考',
                    position: 'left',
                },
                { to: '/blog', label: '项目博客', position: 'left' }, // 用于发布更新日志、技术分享等
                {
                    href: 'https://github.com/liusxml/personal-blog-backend',
                    label: 'GitHub',
                    position: 'right',
                },
            ],
        },

        // --- 6. 页脚定制 ---
        footer: {
            style: 'dark',
            links: [
                {
                    title: '文档',
                    items: [
                        {
                            label: '快速上手',
                            to: '/docs/getting-started/intro',  // 修复：更新为新路径
                        },
                        {
                            label: 'API 参考',
                            to: '/docs/api/overview',
                        },
                    ],
                },
                {
                    title: '社区',
                    items: [
                        {
                            label: '提交 Issue',
                            href: 'https://github.com/liusxml/personal-blog-backend/issues',
                        },
                        {
                            label: '发起讨论',
                            href: 'https://github.com/liusxml/personal-blog-backend/discussions',
                        },
                    ],
                },
                {
                    title: '更多',
                    items: [
                        {
                            label: '项目博客',
                            to: '/blog',
                        },
                        {
                            label: 'GitHub',
                            href: 'https://github.com/liusxml/personal-blog-backend',
                        },
                    ],
                },
            ],
            copyright: `Copyright © ${new Date().getFullYear()} 个人博客后台系统. Built with Docusaurus.`,
        },

        // --- 7. 代码块主题 ---
        prism: {
            theme: prismThemes.github,
            darkTheme: prismThemes.dracula,
            // 增加对 Java 和 Maven POM 的语法高亮支持
            additionalLanguages: ['java', 'bash', 'json', 'markup'],
        },
    } satisfies Preset.ThemeConfig,
};

export default config;

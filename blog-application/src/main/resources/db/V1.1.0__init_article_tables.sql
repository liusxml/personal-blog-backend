-- ============================================================================
-- Article Module Database Schema
-- Version: 1.1.0
-- Description: Initialize article module tables including:
--   - art_article: 文章主表
--   - art_article_stats: 文章统计表（读写分离优化）
--   - art_category: 文章分类表（支持多级分类）
--   - art_tag: 标签表
--   - art_article_tag: 文章-标签关联表
-- 
-- Design Highlights:
--   - MySQL 9.4 VECTOR field for semantic search
--   - Separated stats table for high-frequency updates
--   - Support for multi-level categories
--   - Full-text search with ngram parser
-- ============================================================================

-- ========================================================================
-- 表 1: art_article (文章主表)
-- ========================================================================
CREATE TABLE IF NOT EXISTS `art_article`
(
    -- ========== 主键 ==========
    `id`
    BIGINT
    NOT
    NULL
    COMMENT
    '文章ID（雪花算法）',

    -- ========== 基本信息 ==========
    `title`
    VARCHAR
(
    255
) NOT NULL COMMENT '文章标题',
    `summary` VARCHAR
(
    500
) COMMENT '文章摘要（自动提取或手动填写）',
    `content` LONGTEXT NOT NULL COMMENT '文章正文（Markdown格式）',
    `content_html` LONGTEXT COMMENT '渲染后HTML（可选，用于提升读取性能）',
    `cover_image` VARCHAR
(
    500
) COMMENT '封面图URL或文件ID',
    `cover_image_id` BIGINT COMMENT '封面图文件ID（外键关联 file_file.id）',

    -- ========== 分类与作者 ==========
    `author_id` BIGINT NOT NULL COMMENT '作者ID（外键 sys_user.id）',
    `category_id` BIGINT COMMENT '分类ID（外键 art_category.id）',

    -- ========== 状态与类型 ==========
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0-草稿, 1-审核中, 2-已发布, 3-已归档',
    `type` TINYINT NOT NULL DEFAULT 1 COMMENT '类型: 1-原创, 2-转载, 3-翻译',

    -- ========== 特性标记 ==========
    `is_top` TINYINT NOT NULL DEFAULT 0 COMMENT '是否置顶（0-否, 1-是）',
    `is_featured` TINYINT NOT NULL DEFAULT 0 COMMENT '是否精选（0-否, 1-是）',
    `is_comment_disabled` TINYINT NOT NULL DEFAULT 0 COMMENT '是否禁止评论（0-否, 1-是）',

    -- ========== 高级特性 ==========
    `password` VARCHAR
(
    100
) COMMENT '访问密码（加密存储，可选）',
    `original_url` VARCHAR
(
    500
) COMMENT '原文链接（转载/翻译时填写）',
    `publish_time` DATETIME COMMENT '发布时间（首次发布时间）',
    `toc_json` TEXT COMMENT '目录结构（JSON格式，自动生成）',

    -- ========== 向量搜索（⭐ MySQL 9.4 新特性）==========
    `embedding` VECTOR
(
    1536
) COMMENT '文章内容向量（用于语义搜索和推荐）',

    -- ========== 公共字段（遵循项目规范）==========
    `version` INT NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
    `create_by` BIGINT COMMENT '创建人ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` BIGINT COMMENT '更新人ID',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0-未删除, 1-已删除）',

    -- ========== 索引设计 ==========
    PRIMARY KEY
(
    `id`
),
    KEY `idx_author_status`
(
    `author_id`,
    `status`,
    `is_deleted`
),
    KEY `idx_category`
(
    `category_id`
),
    KEY `idx_publish_time`
(
    `publish_time`
),
    KEY `idx_status_top`
(
    `status`,
    `is_top`,
    `is_deleted`
),
    KEY `idx_create_time`
(
    `create_time`
),
    FULLTEXT KEY `idx_fulltext`
(
    `title`,
    `summary`
)
                                                              WITH PARSER ngram
                                                                  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE =utf8mb4_unicode_ci
    COMMENT='文章主表';

-- ========================================================================
-- 表 2: art_article_stats (文章统计表)
-- 设计原因：将高频更新的统计数据独立拆分，避免主表行锁竞争，提升并发性能
-- ========================================================================
CREATE TABLE IF NOT EXISTS `art_article_stats`
(
    -- ========== 主键 ==========
    `id`
    BIGINT
    NOT
    NULL
    COMMENT
    '主键ID（雪花算法）',
    `article_id`
    BIGINT
    NOT
    NULL
    COMMENT
    '文章ID（外键 art_article.id）',

    -- ========== 统计数据 ==========
    `view_count`
    BIGINT
    NOT
    NULL
    DEFAULT
    0
    COMMENT
    '浏览量',
    `like_count`
    INT
    NOT
    NULL
    DEFAULT
    0
    COMMENT
    '点赞数',
    `comment_count`
    INT
    NOT
    NULL
    DEFAULT
    0
    COMMENT
    '评论数',
    `collect_count`
    INT
    NOT
    NULL
    DEFAULT
    0
    COMMENT
    '收藏数',
    `share_count`
    INT
    NOT
    NULL
    DEFAULT
    0
    COMMENT
    '分享数',

    -- ========== 公共字段 ==========
    `version`
    INT
    NOT
    NULL
    DEFAULT
    1
    COMMENT
    '乐观锁版本号',
    `create_by`
    BIGINT
    COMMENT
    '创建人ID',
    `create_time`
    DATETIME
    NOT
    NULL
    DEFAULT
    CURRENT_TIMESTAMP
    COMMENT
    '创建时间',
    `update_by`
    BIGINT
    COMMENT
    '更新人ID',
    `update_time`
    DATETIME
    NOT
    NULL
    DEFAULT
    CURRENT_TIMESTAMP
    ON
    UPDATE
    CURRENT_TIMESTAMP
    COMMENT
    '更新时间',
    `is_deleted`
    TINYINT
    NOT
    NULL
    DEFAULT
    0
    COMMENT
    '逻辑删除标志',

    -- ========== 索引 ==========
    PRIMARY
    KEY
(
    `id`
),
    UNIQUE KEY `uk_article`
(
    `article_id`
)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE =utf8mb4_unicode_ci
    COMMENT='文章统计表（读写分离优化）';

-- ========================================================================
-- 表 3: art_category (分类表)
-- 支持多级分类，使用 parent_id 和 path 字段实现父子关系
-- ========================================================================
CREATE TABLE IF NOT EXISTS `art_category`
(
    -- ========== 主键 ==========
    `id`
    BIGINT
    NOT
    NULL
    COMMENT
    '分类ID（雪花算法）',

    -- ========== 基本信息 ==========
    `name`
    VARCHAR
(
    50
) NOT NULL COMMENT '分类名称',
    `slug` VARCHAR
(
    50
) NOT NULL COMMENT 'URL友好标识（用于生成链接）',
    `icon` VARCHAR
(
    100
) COMMENT '分类图标（图标名称或URL）',
    `description` VARCHAR
(
    200
) COMMENT '分类描述',

    -- ========== 层级关系 ==========
    `parent_id` BIGINT COMMENT '父分类ID（NULL表示顶级分类）',
    `path` VARCHAR
(
    500
) COMMENT '分类路径（便于查询子分类，格式: /1/2/3）',

    -- ========== 排序 ==========
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序权重（数字越小越靠前）',

    -- ========== 公共字段 ==========
    `version` INT NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
    `create_by` BIGINT COMMENT '创建人ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` BIGINT COMMENT '更新人ID',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标志',

    -- ========== 索引 ==========
    PRIMARY KEY
(
    `id`
),
    UNIQUE KEY `uk_slug`
(
    `slug`
),
    KEY `idx_parent`
(
    `parent_id`
),
    KEY `idx_sort_order`
(
    `sort_order`
)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE =utf8mb4_unicode_ci
    COMMENT='文章分类表（支持多级分类）';

-- ========================================================================
-- 表 4: art_tag (标签表)
-- ========================================================================
CREATE TABLE IF NOT EXISTS `art_tag`
(
    -- ========== 主键 ==========
    `id`
    BIGINT
    NOT
    NULL
    COMMENT
    '标签ID（雪花算法）',

    -- ========== 基本信息 ==========
    `name`
    VARCHAR
(
    50
) NOT NULL COMMENT '标签名称',
    `slug` VARCHAR
(
    50
) NOT NULL COMMENT 'URL标识',
    `color` VARCHAR
(
    20
) COMMENT '标签颜色（HEX格式，如 #3B82F6）',
    `description` VARCHAR
(
    200
) COMMENT '标签描述',

    -- ========== 统计 ==========
    `article_count` INT NOT NULL DEFAULT 0 COMMENT '关联文章数量',

    -- ========== 公共字段 ==========
    `version` INT NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
    `create_by` BIGINT COMMENT '创建人ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` BIGINT COMMENT '更新人ID',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标志',

    -- ========== 索引 ==========
    PRIMARY KEY
(
    `id`
),
    UNIQUE KEY `uk_slug`
(
    `slug`
),
    KEY `idx_name`
(
    `name`
)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE =utf8mb4_unicode_ci
    COMMENT='标签表';

-- ========================================================================
-- 表 5: art_article_tag (文章-标签关联表)
-- 多对多关系中间表
-- ========================================================================
CREATE TABLE IF NOT EXISTS `art_article_tag`
(
    -- ========== 主键 ==========
    `id`
    BIGINT
    NOT
    NULL
    COMMENT
    '主键ID（雪花算法）',

    -- ========== 关系数据 ==========
    `article_id`
    BIGINT
    NOT
    NULL
    COMMENT
    '文章ID（外键 art_article.id）',
    `tag_id`
    BIGINT
    NOT
    NULL
    COMMENT
    '标签ID（外键 art_tag.id）',

    -- ========== 索引 ==========
    PRIMARY
    KEY
(
    `id`
),
    UNIQUE KEY `uk_article_tag`
(
    `article_id`,
    `tag_id`
),
    KEY `idx_tag`
(
    `tag_id`
)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE =utf8mb4_unicode_ci
    COMMENT='文章-标签关联表';

-- ========================================================================
-- 初始化数据（可选）
-- ========================================================================

-- 插入默认分类（使用 INSERT IGNORE 避免重复执行时报错）
INSERT
IGNORE INTO `art_category` (`id`, `name`, `slug`, `description`, `parent_id`, `path`, `sort_order`, `create_time`)
VALUES 
    (1, '技术', 'tech', '技术相关文章', NULL, '/1', 1, NOW()),
    (2, '生活', 'life', '生活随笔', NULL, '/2', 2, NOW()),
    (3, 'Java', 'java', 'Java编程', 1, '/1/3', 1, NOW()),
    (4, 'Spring', 'spring', 'Spring框架', 1, '/1/4', 2, NOW());

-- 插入默认标签（使用 INSERT IGNORE 避免重复执行时报错）
INSERT
IGNORE INTO `art_tag` (`id`, `name`, `slug`, `color`, `create_time`)
VALUES
    (1, 'Java', 'java', '#3B82F6', NOW()),
    (2, 'Spring Boot', 'spring-boot', '#10B981', NOW()),
    (3, 'MySQL', 'mysql', '#F59E0B', NOW()),
    (4, 'Redis', 'redis', '#EF4444', NOW());

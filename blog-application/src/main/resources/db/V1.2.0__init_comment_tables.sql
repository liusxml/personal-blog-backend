-- ========================================================
-- 文件名: V1.2.0__init_comment_tables.sql
-- 描述: 创建评论表（包含树形结构字段）
-- 作者: liusxml
-- 日期: 2025-12-15
-- 版本: 1.2.0
-- ========================================================

USE blog_db;

CREATE TABLE IF NOT EXISTS `cmt_comment` (
    -- 主键
    `id` BIGINT NOT NULL COMMENT '评论ID (主键, 雪花算法)',
    
    -- 关联字段
    `target_type` VARCHAR(20) NOT NULL COMMENT '评论目标类型 (ARTICLE)',
    `target_id` BIGINT NOT NULL COMMENT '目标ID (文章ID)',
    `parent_id` BIGINT NULL COMMENT '父评论ID (NULL表示顶级评论)',
    
    -- 内容字段
    `content` TEXT NOT NULL COMMENT '评论原始内容',
    
    -- 状态字段
    `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态 (1-已通过)',
    
    -- 统计字段
    `like_count` INT NOT NULL DEFAULT 0 COMMENT '点赞数',
    `reply_count` INT NOT NULL DEFAULT 0 COMMENT '回复数',
    
    -- 树形结构字段 (Materialized Path)
    `path` VARCHAR(500) NULL COMMENT '物化路径 (例: /1/2/5/)',
    `depth` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '评论深度 (0-根评论, 1-一级回复, ...)',
    `root_id` BIGINT NULL COMMENT '根评论ID (顶级评论时为自身ID)',
    
    -- 审计字段
    `version` INT NOT NULL DEFAULT 1 COMMENT '版本号 (乐观锁)',
    `create_by` BIGINT NOT NULL COMMENT '创建者ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` BIGINT NULL COMMENT '更新者ID',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    
    -- 索引
    PRIMARY KEY (`id`),
    KEY `idx_target` (`target_type`, `target_id`, `status`),
    KEY `idx_user` (`create_by`, `create_time`),
    KEY `idx_path` (`path`),
    KEY `idx_root_depth` (`root_id`, `depth`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论表';

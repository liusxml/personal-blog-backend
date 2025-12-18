-- Phase 6: 评论编辑历史表
-- 存储评论的编辑历史记录

USE blog_db;

CREATE TABLE IF NOT EXISTS `cmt_comment_history` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `comment_id` BIGINT NOT NULL COMMENT '评论ID',
    `old_content` TEXT NOT NULL COMMENT '编辑前的原始Markdown内容',
    `old_content_html` TEXT NULL COMMENT '编辑前的HTML内容',
    `edit_reason` VARCHAR(200) NULL COMMENT '编辑原因（用户可选填写）',
    
    -- 审计字段
    `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `create_by` BIGINT NULL COMMENT '编辑人ID（冗余存储）',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '编辑时间',
    `update_by` BIGINT NULL COMMENT '更新人',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    
    PRIMARY KEY (`id`),
    KEY `idx_comment_id` (`comment_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论编辑历史表';

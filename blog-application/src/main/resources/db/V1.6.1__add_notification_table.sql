-- Phase 6: 系统通知表
-- 存储用户通知（回复通知、@提及通知等）

USE
blog_db;

CREATE TABLE IF NOT EXISTS `sys_notification`
(
    `id`
    BIGINT
    NOT
    NULL
    COMMENT
    '主键ID',
    `user_id`
    BIGINT
    NOT
    NULL
    COMMENT
    '接收通知的用户ID',
    `type`
    VARCHAR
(
    30
) NOT NULL COMMENT '通知类型：COMMENT_REPLY-回复通知，USER_MENTION-@提及通知，COMMENT_LIKE-点赞通知',
    `title` VARCHAR
(
    100
) NOT NULL COMMENT '通知标题',
    `content` VARCHAR
(
    500
) NULL COMMENT '通知内容',
    `source_id` BIGINT NULL COMMENT '来源ID（评论ID、点赞ID等）',
    `source_type` VARCHAR
(
    30
) NULL COMMENT '来源类型：COMMENT, LIKE',
    `is_read` TINYINT NOT NULL DEFAULT 0 COMMENT '是否已读：0-未读，1-已读',
    `read_time` DATETIME NULL COMMENT '阅读时间',

    -- 审计字段
    `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `create_by` BIGINT NULL COMMENT '触发人ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` BIGINT NULL COMMENT '更新人',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY
(
    `id`
),
    KEY `idx_user_id`
(
    `user_id`
),
    KEY `idx_is_read`
(
    `is_read`
),
    KEY `idx_type`
(
    `type`
),
    KEY `idx_create_time`
(
    `create_time`
),
    KEY `idx_source`
(
    `source_type`,
    `source_id`
)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE =utf8mb4_unicode_ci COMMENT='系统通知表';

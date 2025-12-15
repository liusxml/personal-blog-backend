-- ========================================================
-- 文件名: V1.5.0__init_comment_like_report_tables.sql
-- 描述: 创建评论点赞和举报表
-- 作者: liusxml
-- 日期: 2025-12-15
-- 版本: 1.5.0
-- ========================================================

USE blog_db;

-- ========================================================
-- 1. 创建评论点赞表
-- ========================================================
CREATE TABLE IF NOT EXISTS `cmt_comment_like` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `comment_id` BIGINT NOT NULL COMMENT '评论ID',
  `user_id` BIGINT NOT NULL COMMENT '点赞用户ID',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `create_by` BIGINT NULL COMMENT '创建人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT NULL COMMENT '更新人',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_comment` (`user_id`, `comment_id`, `is_deleted`),
  KEY `idx_comment_id` (`comment_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论点赞表';

-- ========================================================
-- 2. 创建评论举报表
-- ========================================================
CREATE TABLE IF NOT EXISTS `cmt_comment_report` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `comment_id` BIGINT NOT NULL COMMENT '被举报评论ID',
  `reporter_id` BIGINT NOT NULL COMMENT '举报人ID',
  `reason_type` VARCHAR(50) NOT NULL COMMENT '举报原因类型：SPAM-垃圾广告，ABUSE-辱骂诽谤，ILLEGAL-违法信息，PORNOGRAPHY-色情低俗，FALSE_INFO-虚假信息，OTHER-其他',
  `reason_detail` VARCHAR(500) NULL COMMENT '举报详细说明',
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '审核状态：PENDING-待审核，APPROVED-有效，REJECTED-无效',
  `admin_remark` VARCHAR(500) NULL COMMENT '管理员备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `create_by` BIGINT NULL COMMENT '创建人（举报人）',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT NULL COMMENT '更新人（审核人）',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_comment_id` (`comment_id`),
  KEY `idx_reporter_id` (`reporter_id`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论举报表';

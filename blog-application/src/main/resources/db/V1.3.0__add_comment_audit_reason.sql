-- ========================================================
-- 文件名: V1.3.0__add_comment_audit_reason.sql
-- 描述: 为评论表添加审核原因字段
-- 作者: liusxml
-- 日期: 2025-12-15
-- 版本: 1.3.0
-- ========================================================

USE blog_db;

-- 添加审核原因字段
ALTER TABLE `cmt_comment`
    ADD COLUMN `audit_reason` VARCHAR(500) NULL COMMENT '审核/删除原因' AFTER `root_id`;

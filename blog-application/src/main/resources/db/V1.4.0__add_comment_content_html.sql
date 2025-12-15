-- ========================================================
-- 文件名: V1.4.0__add_comment_content_html.sql
-- 描述: 添加评论 HTML 渲染字段
-- 作者: liusxml
-- 日期: 2025-12-15
-- 版本: 1.4.0
-- ========================================================

USE blog_db;

-- -- 添加 content_html 字段
-- ALTER TABLE `cmt_comment`
--     ADD COLUMN `content_html` TEXT NULL COMMENT '渲染后的 HTML 内容' AFTER `content`;

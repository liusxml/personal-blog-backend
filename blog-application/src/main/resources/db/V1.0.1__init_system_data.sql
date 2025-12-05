-- ========================================================
-- 文件名: V1.0.1__init_system_data.sql
-- 描述: 初始化系统模块数据（角色和管理员账户）
-- 作者: liusxml
-- 日期: 2025-12-04
-- ========================================================

-- ========== 1. 插入系统角色 ==========
INSERT IGNORE INTO sys_role (id, role_name, role_key, status, version, create_time, update_time, is_deleted, remark)
VALUES
    (1, '管理员', 'ADMIN', 1, 0, NOW(), NOW(), 0, '系统管理员，拥有所有权限'),
    (2, '作者', 'AUTHOR', 1, 0, NOW(), NOW(), 0, '可以发布和管理文章'),
    (3, '用户', 'USER', 1, 0, NOW(), NOW(), 0, '普通用户，可以浏览和评论');

-- ========== 2. 插入管理员账户 ==========
-- 密码: Admin@123 (BCrypt 加密后)
INSERT IGNORE INTO sys_user (id, username, nickname, password, email, avatar, status, version, create_time, update_time, is_deleted, remark)
VALUES
    (1, 'admin', '系统管理员',
     '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi',
     'admin@blog.com',
     NULL,
     1,
     0,
     NOW(),
     NOW(),
     0,
     '系统初始管理员账户');

-- ========== 3. 为管理员分配角色 ==========
INSERT IGNORE INTO sys_user_role (user_id, role_id)
VALUES
    (1, 1), -- ADMIN
    (1, 2), -- AUTHOR
    (1, 3); -- USER

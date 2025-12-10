-- ========================================================
-- 文件名: V1.0.0__init_schema.sql
-- 描述: 初始化数据库表结构（系统模块）
-- 作者: liusxml
-- 日期: 2025-12-08
-- 版本: 2.0 (优化版)
-- ========================================================

-- 确保在正确的数据库中执行
USE blog_db;

-- ========================================================
-- 1. 用户信息表 (sys_user)
-- ========================================================
CREATE TABLE IF NOT EXISTS `sys_user` (
    `id` BIGINT NOT NULL COMMENT '用户ID (主键, 雪花算法)',
    `username` VARCHAR(64) NOT NULL COMMENT '用户名 (登录凭证, 唯一)',
    `nickname` VARCHAR(64) NOT NULL COMMENT '用户昵称',
    `password` VARCHAR(255) NOT NULL COMMENT '加密后的密码 (BCrypt)',
    `email` VARCHAR(128) NULL COMMENT '用户邮箱 (唯一)',
    `avatar` VARCHAR(255) NULL COMMENT '用户头像URL',
    `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '账户状态 (1-正常, 0-禁用)',
    `version` INT NOT NULL DEFAULT 1 COMMENT '版本号 (乐观锁)',
    `create_by` BIGINT NULL COMMENT '创建者ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` BIGINT NULL COMMENT '更新者ID',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除 (0-未删, 1-已删)',
    `remark` VARCHAR(500) NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_email` (`email`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户信息表';

-- ========================================================
-- 2. 角色信息表 (sys_role)
-- ========================================================
CREATE TABLE IF NOT EXISTS `sys_role` (
    `id` BIGINT NOT NULL COMMENT '角色ID (主键, 雪花算法)',
    `role_name` VARCHAR(128) NOT NULL COMMENT '角色名称',
    `role_key` VARCHAR(128) NOT NULL COMMENT '角色权限字符串 (唯一)',
    `role_sort` INT NOT NULL DEFAULT 0 COMMENT '角色排序 (数字越小越靠前)',
    `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '角色状态 (1-正常, 0-停用)',
    `version` INT NOT NULL DEFAULT 0 COMMENT '版本号 (乐观锁)',
    `create_by` BIGINT NULL COMMENT '创建者ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` BIGINT NULL COMMENT '更新者ID',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除 (0-未删, 1-已删)',
    `remark` VARCHAR(500) NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_key` (`role_key`),
    KEY `idx_status` (`status`),
    KEY `idx_role_sort` (`role_sort`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色信息表';

-- ========================================================
-- 3. 用户角色关联表 (sys_user_role)
-- ========================================================
CREATE TABLE IF NOT EXISTS `sys_user_role` (
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `role_id` BIGINT NOT NULL COMMENT '角色ID',
    PRIMARY KEY (`user_id`, `role_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

-- ========================================================
-- 说明
-- ========================================================
-- 1. 所有表使用 InnoDB 引擎，支持事务和外键
-- 2. 字符集使用 utf8mb4，支持 Emoji 等特殊字符
-- 3. 添加了性能优化索引：
--    - 唯一索引：username, email, role_key
--    - 普通索引：status, create_time, role_sort
-- 4. 所有表包含审计字段：create_by, create_time, update_by, update_time
-- 5. 所有表支持逻辑删除：is_deleted
-- 6. 所有表支持乐观锁：version
-- 7. sys_role 新增 role_sort 字段用于排序

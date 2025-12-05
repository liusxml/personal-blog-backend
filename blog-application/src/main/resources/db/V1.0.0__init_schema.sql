-- Blog Project DDL Scripts - v1.0.0
-- Description: Initial database schema setup for all core tables.
-- Author: liusxml
-- Date: 2025-09-20

-- ----------------------------
-- Table structure for sys_user (用户信息表)
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sys_user` (
                                          `id` BIGINT NOT NULL COMMENT '用户ID (主键, 雪花算法)',
                                          `username` VARCHAR(64) NOT NULL UNIQUE COMMENT '用户名 (登录凭证)',
                                          `nickname` VARCHAR(64) NOT NULL COMMENT '用户昵称',
                                          `password` VARCHAR(255) NOT NULL COMMENT '加密后的密码',
                                          `email` VARCHAR(128) NULL UNIQUE COMMENT '用户邮箱',
                                          `avatar` VARCHAR(255) NULL COMMENT '用户头像URL',
                                          `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '账户状态 (1-正常, 0-禁用)',
                                          `version` INT NOT NULL DEFAULT 1 COMMENT '版本号 (乐观锁)',
                                          `create_by` BIGINT NULL COMMENT '创建者ID',
                                          `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                          `update_by` BIGINT NULL COMMENT '更新者ID',
                                          `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                          `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除 (0-未删, 1-已删)',
                                          `remark` VARCHAR(500) NULL COMMENT '备注',
                                          PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户信息表';

-- ----------------------------
-- Table structure for sys_role (角色信息表)
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sys_role` (
                                          `id` BIGINT NOT NULL COMMENT '角色ID (主键, 雪花算法)',
                                          `role_name` VARCHAR(128) NOT NULL COMMENT '角色名称',
                                          `role_key` VARCHAR(128) NOT NULL UNIQUE COMMENT '角色权限字符串',
                                          `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '角色状态 (1-正常, 0-停用)',
                                          `version` INT NOT NULL DEFAULT 0 COMMENT '版本号 (乐观锁)',
                                          `create_by` BIGINT NULL COMMENT '创建者ID',
                                          `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                          `update_by` BIGINT NULL COMMENT '更新者ID',
                                          `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                          `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除 (0-未删, 1-已删)',
                                          `remark` VARCHAR(500) NULL COMMENT '备注',
                                          PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色信息表';

-- ----------------------------
-- Table structure for sys_user_role (用户和角色关联表)
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sys_user_role` (
                                               `user_id` BIGINT NOT NULL COMMENT '用户ID',
                                               `role_id` BIGINT NOT NULL COMMENT '角色ID',
                                               PRIMARY KEY (`user_id`, `role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户和角色关联表';

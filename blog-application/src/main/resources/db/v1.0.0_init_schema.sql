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
                                          `version` INT NOT NULL DEFAULT 1 COMMENT '版本号 (乐观锁)',
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

-- ----------------------------
-- Table structure for bl_category (文章分类表)
-- ----------------------------
CREATE TABLE IF NOT EXISTS `bl_category` (
                                             `id` BIGINT NOT NULL COMMENT '分类ID (主键, 雪花算法)',
                                             `name` VARCHAR(255) NOT NULL UNIQUE COMMENT '分类名',
                                             `description` VARCHAR(1024) NULL COMMENT '分类描述',
                                             `pid` BIGINT NOT NULL DEFAULT 0 COMMENT '父分类ID (0为根分类)',
                                             `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态 (1-正常, 0-禁用)',
                                             `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                             `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                             `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除 (0-未删, 1-已删)',
                                             PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章分类表';

-- ----------------------------
-- Table structure for bl_tag (文章标签表)
-- ----------------------------
CREATE TABLE IF NOT EXISTS `bl_tag` (
                                        `id` BIGINT NOT NULL COMMENT '标签ID (主键, 雪花算法)',
                                        `name` VARCHAR(255) NOT NULL UNIQUE COMMENT '标签名',
                                        `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                        `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                        `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除 (0-未删, 1-已删)',
                                        PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章标签表';

-- ----------------------------
-- Table structure for bl_article (文章表)
-- ----------------------------
CREATE TABLE IF NOT EXISTS `bl_article` (
                                            `id` BIGINT NOT NULL COMMENT '文章ID (主键, 雪花算法)',
                                            `title` VARCHAR(255) NOT NULL COMMENT '文章标题',
                                            `content` LONGTEXT NOT NULL COMMENT '文章内容',
                                            `summary` VARCHAR(1024) NULL COMMENT '文章摘要',
                                            `category_id` BIGINT NOT NULL COMMENT '所属分类ID',
                                            `thumbnail` VARCHAR(255) NULL COMMENT '缩略图URL',
                                            `is_top` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否置顶 (0-否, 1-是)',
                                            `status` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '状态 (0-草稿, 1-已发布)',
                                            `view_count` BIGINT NOT NULL DEFAULT 0 COMMENT '访问量',
                                            `allow_comment` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否允许评论 (1-是, 0-否)',
                                            `version` INT NOT NULL DEFAULT 1 COMMENT '版本号 (乐观锁)',
                                            `create_by` BIGINT NULL COMMENT '创建者ID',
                                            `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                            `update_by` BIGINT NULL COMMENT '更新者ID',
                                            `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                            `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除 (0-未删, 1-已删)',
                                            PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章表';

-- ----------------------------
-- Table structure for bl_article_tag (文章和标签关联表)
-- ----------------------------
CREATE TABLE IF NOT EXISTS `bl_article_tag` (
                                                `article_id` BIGINT NOT NULL COMMENT '文章ID',
                                                `tag_id` BIGINT NOT NULL COMMENT '标签ID',
                                                PRIMARY KEY (`article_id`, `tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章和标签关联表';

-- ----------------------------
-- Table structure for bl_comment (评论表)
-- ----------------------------
CREATE TABLE IF NOT EXISTS `bl_comment` (
                                            `id` BIGINT NOT NULL COMMENT '评论ID (主键, 雪花算法)',
                                            `article_id` BIGINT NOT NULL COMMENT '所属文章ID',
                                            `root_id` BIGINT NOT NULL DEFAULT 0 COMMENT '根评论ID (0表示自身是根评论)',
                                            `to_comment_user_id` BIGINT NULL COMMENT '回复的目标用户ID',
                                            `to_comment_id` BIGINT NULL COMMENT '回复的目标评论ID',
                                            `content` TEXT NOT NULL COMMENT '评论内容',
                                            `create_by` BIGINT NOT NULL COMMENT '评论者ID',
                                            `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评论时间',
                                            `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                            `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除 (0-未删, 1-已删)',
                                            PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论表';

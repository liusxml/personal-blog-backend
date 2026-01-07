-- ============================================================================
-- File Module Database Schema (Optimized)
-- Version: 1.0.2
-- Description: Create file_file table with optimized schema based on best practices
-- Changes from initial design:
--   - Removed access_url (should be dynamically generated)
--   - Added ref_type/ref_id for business association
--   - Added file_category, image metadata, usage statistics
--   - Changed MD5 index from UNIQUE to regular KEY
-- ============================================================================

-- 文件记录表（优化版）
CREATE TABLE IF NOT EXISTS `file_file`
(
    -- ========== 主键 ==========
    `id`
    BIGINT
    NOT
    NULL
    COMMENT
    '文件ID（雪花算法）',

    -- ========== 存储信息 ==========
    `file_key`
    VARCHAR
(
    255
) NOT NULL COMMENT '存储键（uploads/2025/12/xxx.jpg）',
    `storage_type` VARCHAR
(
    20
) DEFAULT 'BITIFUL' COMMENT '存储类型：BITIFUL、ALIYUN、MINIO、LOCAL',
    `bucket` VARCHAR
(
    100
) COMMENT 'Bucket 名称',

    -- ========== 文件元数据 ==========
    `original_name` VARCHAR
(
    255
) NOT NULL COMMENT '原始文件名',
    `file_size` BIGINT NOT NULL COMMENT '文件大小（字节）',
    `content_type` VARCHAR
(
    150
) COMMENT 'MIME类型',
    `extension` VARCHAR
(
    20
) COMMENT '扩展名（jpg、png）',
    `md5` VARCHAR
(
    32
) COMMENT 'MD5值（用于秒传）',

    -- ========== 文件分类 ==========
    `file_category` VARCHAR
(
    20
) COMMENT '文件分类：IMAGE、VIDEO、DOCUMENT、AUDIO、OTHER',

    -- ========== 图片专属元信息 ==========
    `image_width` INT COMMENT '图片宽度（像素）',
    `image_height` INT COMMENT '图片高度（像素）',

    -- ========== 业务关联 ==========
    `ref_type` VARCHAR
(
    50
) COMMENT '引用类型：ARTICLE、COMMENT、AVATAR、ATTACHMENT',
    `ref_id` BIGINT COMMENT '引用对象ID',

    -- ========== 访问控制 ==========
    `access_policy` VARCHAR
(
    20
) DEFAULT 'PRIVATE' COMMENT '访问策略：PRIVATE、PUBLIC',
    `cdn_url` VARCHAR
(
    255
) COMMENT 'CDN URL（仅PUBLIC文件，可选）',

    -- ========== 上传状态 ==========
    `upload_status` TINYINT DEFAULT 0 COMMENT '上传状态：0=待上传、1=已完成、2=失败',
    `upload_complete_time` DATETIME COMMENT '上传完成时间',

    -- ========== 使用统计 ==========
    `download_count` INT DEFAULT 0 COMMENT '下载次数',
    `view_count` INT DEFAULT 0 COMMENT '查看次数',

    -- ========== Bitiful CoreIX ==========
    `process_params` VARCHAR
(
    500
) COMMENT 'CoreIX处理参数（如：w=300&fmt=webp&q=80）',

    -- ========== 公共字段 ==========
    `version` INT DEFAULT 0 COMMENT '乐观锁版本',
    `create_by` BIGINT COMMENT '创建人ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` BIGINT COMMENT '更新人ID',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除：0=未删除、1=已删除',

    -- ========== 索引 ==========
    PRIMARY KEY
(
    `id`
),
    UNIQUE KEY `uk_file_key`
(
    `file_key`
),
    KEY `idx_md5_size`
(
    `md5`,
    `file_size`
), -- 秒传索引（允许NULL）
    KEY `idx_ref`
(
    `ref_type`,
    `ref_id`
), -- 业务关联索引
    KEY `idx_category_status`
(
    `file_category`,
    `upload_status`
), -- 分类查询
    KEY `idx_create_by`
(
    `create_by`
),
    KEY `idx_upload_status`
(
    `upload_status`
),
    KEY `idx_create_time`
(
    `create_time`
)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件记录表（优化版）';


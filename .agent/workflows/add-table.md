---
description: 如何添加新的数据库表（Flyway + Entity + Mapper）
---

# 添加新数据库表

## 前置条件

- 确定表名（使用正确前缀：`sys_`, `art_`, `cmt_`, `file_`）
- 确定所属模块
- 设计表结构

## 操作步骤

### 1. 创建 Flyway 迁移脚本

在 `blog-application/src/main/resources/db/` 下创建：

- 文件名格式：`V{major}.{minor}.{patch}__{description}.sql`
- 示例：`V1.0.3__create_art_article.sql`

```sql
-- ========================================================
-- 文件名: V1.0.3__create_art_article.sql
-- 描述: 创建文章表
-- 作者: your_name
-- 日期: YYYY-MM-DD
-- ========================================================

USE blog_db;

CREATE TABLE IF NOT EXISTS `art_article` (
    -- 主键
    `id` BIGINT NOT NULL COMMENT '文章ID (主键, 雪花算法)',
    
    -- 业务字段
    `title` VARCHAR(255) NOT NULL COMMENT '文章标题',
    `content` LONGTEXT NOT NULL COMMENT '文章内容',
    `status` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '状态 (0-草稿, 1-发布)',
    
    -- 审计字段 (必须包含)
    `version` INT NOT NULL DEFAULT 1 COMMENT '版本号 (乐观锁)',
    `create_by` BIGINT NULL COMMENT '创建者ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` BIGINT NULL COMMENT '更新者ID',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除 (0-未删, 1-已删)',
    `remark` VARCHAR(500) NULL COMMENT '备注',
    
    -- 索引
    PRIMARY KEY (`id`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章信息表';
```

### 2. 创建 Entity

在 `*-service` 模块创建 (`entity/ArtArticle.java`):

```java
@Data
@TableName("art_article")
public class ArtArticle {
    
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    private String title;
    private String content;
    private Integer status;
    
    @Version
    private Integer version;
    
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @TableLogic
    private Integer isDeleted;
    
    private String remark;
}
```

### 3. 创建 Mapper

**接口** (`mapper/ArticleMapper.java`):

```java
@Mapper
public interface ArticleMapper extends BaseMapper<ArtArticle> {
}
```

**XML** (可选，`resources/mapper/ArticleMapper.xml`):

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.blog.file.mapper.ArticleMapper">
</mapper>
```

// turbo

### 4. 验证数据库迁移

```bash
# 启动应用，Flyway 会自动执行迁移
mvn spring-boot:run -pl blog-application
```

### 5. 验证表结构

```sql
DESCRIBE art_article;
SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 1;
```

## 检查清单

- [ ] SQL 脚本版本号正确递增
- [ ] 表名使用正确前缀
- [ ] 包含所有审计字段
- [ ] Entity 字段与表结构一致
- [ ] `@TableLogic` 和 `@Version` 注解正确
- [ ] Flyway 迁移成功

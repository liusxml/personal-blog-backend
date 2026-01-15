package com.blog.file.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件实体（优化版）
 *
 * <p>
 * 遵循项目规范 + 最佳实践：
 * <ul>
 * <li>使用 MyBatis-Plus 注解</li>
 * <li>包含所有 7 个公共字段</li>
 * <li>使用雪花算法生成 ID</li>
 * <li>支持乐观锁和逻辑删除</li>
 * <li>添加业务关联、分类、统计等字段</li>
 * </ul>
 * </p>
 *
 * <p>
 * <strong>优化点</strong>：
 * <ul>
 * <li>移除 accessUrl（改为动态生成）</li>
 * <li>添加 refType/refId（业务关联）</li>
 * <li>添加 fileCategory（文件分类）</li>
 * <li>添加图片元信息（width/height）</li>
 * <li>添加使用统计（downloadCount/viewCount）</li>
 * </ul>
 * </p>
 *
 * @author liusxml
 * @since 1.0.0
 */
@Data
@TableName("file_file")
public class FileEntity {

    // ========== 主键 ==========

    /**
     * 文件ID（雪花算法自动生成）
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    // ========== 存储信息 ==========

    /**
     * 存储键（uploads/2025/12/xxx.jpg）
     */
    private String fileKey;

    /**
     * 存储类型：BITIFUL、ALIYUN、MINIO、LOCAL
     */
    private String storageType;

    /**
     * Bucket 名称
     */
    private String bucket;

    // ========== 文件元数据 ==========

    /**
     * 原始文件名
     */
    private String originalName;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * MIME类型
     */
    private String contentType;

    /**
     * 扩展名（jpg、png）
     */
    private String extension;

    /**
     * MD5值（用于秒传）
     */
    private String md5;

    // ========== 文件分类 ==========

    /**
     * 文件分类：IMAGE、VIDEO、DOCUMENT、AUDIO、OTHER
     */
    private String fileCategory;

    // ========== 图片专属元信息 ==========

    /**
     * 图片宽度（像素）
     */
    private Integer imageWidth;

    /**
     * 图片高度（像素）
     */
    private Integer imageHeight;

    // ========== 业务关联 ==========

    /**
     * 引用类型：ARTICLE、COMMENT、AVATAR、ATTACHMENT
     */
    private String refType;

    /**
     * 引用对象ID
     */
    private Long refId;

    // ========== 访问控制 ==========

    /**
     * 访问策略：PRIVATE、PUBLIC
     */
    private String accessPolicy;

    /**
     * CDN URL（仅PUBLIC文件，可选）
     */
    private String cdnUrl;

    // ========== 上传状态 ==========

    /**
     * 上传状态：0=待上传、1=已完成、2=失败
     */
    private Integer uploadStatus;

    /**
     * 上传完成时间
     */
    private LocalDateTime uploadCompleteTime;

    // ========== 使用统计 ==========

    /**
     * 下载次数
     */
    private Integer downloadCount;

    /**
     * 查看次数
     */
    private Integer viewCount;

    // ========== Bitiful CoreIX ==========

    /**
     * CoreIX处理参数（如：w=300&fmt=webp&q=80）
     */
    private String processParams;

    // ========== 公共字段（必须包含）==========

    /**
     * 乐观锁版本
     */
    @Version
    private Integer version;

    /**
     * 创建人ID
     */
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新人ID
     */
    @TableField(fill = FieldFill.UPDATE)
    private Long updateBy;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除：0=未删除、1=已删除
     */
    @TableLogic
    private Integer isDeleted;
}

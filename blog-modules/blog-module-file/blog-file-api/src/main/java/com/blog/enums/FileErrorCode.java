package com.blog.enums;

import com.blog.common.exception.ErrorCode;
import lombok.Getter;

/**
 * 文件模块错误码枚举类
 * <p>
 * 实现 {@link ErrorCode} 接口，提供文件相关操作的标准化错误码。
 * 错误码设计规范：
 * <ul>
 *   <li><strong>code</strong>：5 位数字，11000 开始递增（11 表示文件模块）</li>
 *   <li><strong>message</strong>：简洁英文描述，便于国际化</li>
 * </ul>
 * 示例使用：
 * <pre>
 * throw new BusinessException(FileErrorCodeEnum.FILE_UPLOAD_FAILED);
 * throw new BusinessException(FileErrorCodeEnum.FILE_NOT_FOUND, "File ID: 123 not exist");
 * </pre>
 * </p>
 * <p>
 * <strong>微服务演进</strong>：此枚举可作为独立 JAR 共享到 file-server 模块，无需修改。
 * </p>
 *
 * @author Your Name
 * @since 1.0-SNAPSHOT
 */
@Getter
public enum FileErrorCode implements ErrorCode {

    /**
     * 文件上传失败（通用）
     * <p>场景：IO 异常、网络问题、SDK 错误</p>
     */
    FILE_UPLOAD_FAILED(11001, "File upload failed"),

    /**
     * 文件类型无效
     * <p>场景：扩展名不在白名单（如 jpg, png）</p>
     */
    FILE_INVALID_TYPE(11002, "Invalid file type"),

    /**
     * 文件大小超过限制
     * <p>场景：文件 > 配置的最大大小（如 10MB）</p>
     */
    FILE_EXCEED_MAX_SIZE(11003, "File exceeds maximum size"),

    /**
     * 文件名无效
     * <p>场景：文件名为空或包含非法字符</p>
     */
    FILE_INVALID_NAME(11004, "Invalid file name"),

    /**
     * 文件已存在（MD5 秒传）
     * <p>场景：相同 MD5 的文件已上传</p>
     */
    FILE_MD5_EXISTS(11005, "File already exists"),

    /**
     * 存储服务异常
     * <p>场景：Bitiful/OSS 等第三方服务错误、限额超支</p>
     */
    FILE_STORAGE_ERROR(11006, "Storage service error"),

    /**
     * 预签名 URL 生成失败
     * <p>场景：S3Presigner 异常或权限不足</p>
     */
    FILE_PRESIGNED_URL_FAILED(11007, "Presigned URL generation failed"),

    /**
     * 文件不存在
     * <p>场景：下载/删除时 key 未找到</p>
     */
    FILE_NOT_FOUND(11008, "File not found"),

    /**
     * 文件下载失败
     * <p>场景：网络、权限、签名过期</p>
     */
    FILE_DOWNLOAD_FAILED(11009, "File download failed"),

    /**
     * 文件删除失败
     * <p>场景：对象不存在或权限不足（非致命）</p>
     */
    FILE_DELETE_FAILED(11010, "File delete failed"),

    /**
     * 存储类型无效
     * <p>场景：oss.type 配置错误（如 BITIFUL 不支持）</p>
     */
    FILE_STORAGE_TYPE_INVALID(11011, "Invalid storage type"),

    /**
     * 存储桶不存在
     * <p>场景：Bucket 未创建或无权限</p>
     */
    FILE_BUCKET_NOT_FOUND(11012, "Bucket not found"),

    /**
     * 预签名 URL 已过期
     * <p>场景：前端上传超时</p>
     */
    FILE_PRESIGNED_URL_EXPIRED(11013, "Presigned URL expired"),

    /**
     * 文件路径生成失败
     * <p>场景：UUID 或日期格式化异常（极少）</p>
     */
    FILE_KEY_GENERATE_FAILED(11014, "File key generation failed");

    /**
     * 错误码（Integer 类型）
     */
    private final Integer code;

    /**
     * 默认错误消息
     */
    private final String message;

    /**
     * 构造方法
     *
     * @param code 错误码
     * @param message 默认消息
     */
    FileErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * 根据 code 获取枚举项
     * <p>
     * 使用 commons-lang3 风格的空校验，便于扩展。
     * </p>
     *
     * @param code 错误码
     * @return 对应的枚举项，或 null
     */
    public static FileErrorCode fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (FileErrorCode item : values()) {
            if (item.getCode().equals(code)) {
                return item;
            }
        }
        return null;
    }
}
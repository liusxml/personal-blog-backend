package com.blog.infrastructure.constant;

/**
 * 文件存储常量类
 * <p>
 * 集中管理文件存储模块的所有常量，避免魔法数字和字符串散落在代码中。
 * 遵循单一职责原则，便于维护和修改。
 * </p>
 *
 * @author liusxml
 * @since 1.0-SNAPSHOT
 */
public final class FileStorageConstants {

    private FileStorageConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // ────────────────────────── 文件大小限制 ──────────────────────────

    /**
     * 默认最大文件大小：10MB
     */
    public static final long DEFAULT_MAX_FILE_SIZE = 10 * 1024 * 1024L;

    /**
     * 最小允许的文件大小：1KB
     */
    public static final long MIN_FILE_SIZE = 1024L;

    // ────────────────────────── 时间配置 ──────────────────────────

    /**
     * 默认预签名 GET URL 过期时间：7 天
     */
    public static final int DEFAULT_PRESIGNED_GET_URL_EXPIRY_DAYS = 7;

    /**
     * 默认预签名 PUT URL 过期时间：30 分钟
     */
    public static final int DEFAULT_PRESIGNED_PUT_URL_EXPIRY_MINUTES = 30;

    /**
     * 最小预签名 URL 过期时间：1 分钟
     */
    public static final int MIN_PRESIGNED_URL_EXPIRY_MINUTES = 1;

    /**
     * 最大预签名 URL 过期时间：60 分钟
     */
    public static final int MAX_PRESIGNED_URL_EXPIRY_MINUTES = 60;

    // ────────────────────────── 文件路径配置 ──────────────────────────

    /**
     * 上传文件路径前缀
     */
    public static final String UPLOAD_PATH_PREFIX = "uploads/";

    /**
     * 日期路径格式：yyyy/MM/dd
     */
    public static final String DATE_PATH_PATTERN = "yyyy/MM/dd";

    // ────────────────────────── 文件扩展名白名单 ──────────────────────────

    /**
     * 默认允许的图片扩展名
     */
    public static final String[] DEFAULT_IMAGE_EXTENSIONS = {
            "jpg", "jpeg", "png", "gif", "webp", "bmp", "svg"
    };

    /**
     * 默认允许的文档扩展名
     */
    public static final String[] DEFAULT_DOCUMENT_EXTENSIONS = {
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt"
    };

    /**
     * 默认允许的所有扩展名（图片 + 文档）
     */
    public static final String DEFAULT_ALLOWED_EXTENSIONS_STRING = "jpg,jpeg,png,gif,webp,pdf,docx";

    // ────────────────────────── 日志消息模板 ──────────────────────────

    /**
     * 文件上传成功日志模板
     */
    public static final String LOG_UPLOAD_SUCCESS = "文件上传成功: bucket={}, key={}, size={}, contentType={}";

    /**
     * 文件上传失败日志模板
     */
    public static final String LOG_UPLOAD_FAILED = "文件上传失败: key={}, error={}";

    /**
     * 文件删除成功日志模板
     */
    public static final String LOG_DELETE_SUCCESS = "文件删除成功: key={}";

    /**
     * 文件删除失败日志模板
     */
    public static final String LOG_DELETE_FAILED = "文件删除失败: key={}, error={}";

    /**
     * 预签名 URL 生成成功日志模板
     */
    public static final String LOG_PRESIGNED_URL_SUCCESS = "预签名 URL 生成成功: key={}, expireMinutes={}";

    /**
     * 预签名 URL 生成失败日志模板
     */
    public static final String LOG_PRESIGNED_URL_FAILED = "预签名 URL 生成失败: key={}, error={}";

    /**
     * 文件大小超限日志模板
     */
    public static final String LOG_FILE_SIZE_EXCEEDED = "文件大小超限: actual={}, max={}, filename={}";

    /**
     * 文件类型无效日志模板
     */
    public static final String LOG_INVALID_FILE_TYPE = "文件类型无效: extension={}, allowed={}";

    // ────────────────────────── 错误消息模板 ──────────────────────────

    /**
     * 文件大小超限错误消息模板
     */
    public static final String ERROR_FILE_SIZE_EXCEEDED = "文件大小 %d 字节超过限制 %d 字节";

    /**
     * 文件类型无效错误消息模板
     */
    public static final String ERROR_INVALID_FILE_TYPE = "不支持的文件类型: %s，允许的类型: %s";

    /**
     * FileKey 为空错误消息
     */
    public static final String ERROR_FILE_KEY_BLANK = "fileKey 不能为空";

    /**
     * 过期时间无效错误消息
     */
    public static final String ERROR_INVALID_EXPIRY_TIME = "过期时间应在 %d-%d 分钟之间";
}

package com.blog.infrastructure.oss;

import com.blog.common.exception.BusinessException;
import com.blog.enums.FileErrorCode;
import com.blog.infrastructure.config.BitifulProperties;
import com.blog.infrastructure.constant.FileStorageConstants;
import com.blog.infrastructure.storage.FileStorageStrategy;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Bitiful 存储策略实现类
 * <p>
 * 实现 {@link FileStorageStrategy} 接口，封装 Bitiful S3 兼容操作。
 * 采用 <strong>策略模式 + 适配器模式</strong>，隔离底层 S3 细节。
 * 支持：
 * <ul>
 * <li>文件上传（含 Content-Type 自动检测）</li>
 * <li>预签名 URL 生成（前端直传）</li>
 * <li>7 天签名访问 URL（上传后返回）</li>
 * <li>文件删除（软删除后调用）</li>
 * <li>大文件分片上传扩展点（MultipartUpload）</li>
 * </ul>
 * </p>
 * <p>
 * <strong>工具类使用：</strong>
 * <ul>
 * <li><code>commons-io</code>：{@link FilenameUtils} 处理文件名</li>
 * <li><code>commons-lang3</code>：{@link StringUtils} 校验参数</li>
 * <li><code>guava</code>：MD5 计算（秒传已在 FileServiceImpl 处理）</li>
 * </ul>
 * </p>
 * <p>
 * <strong>微服务演进：</strong>
 * <ul>
 * <li>单体阶段：注入到 {@link com.blog.file.service.impl.FileServiceImpl}</li>
 * <li>微服务阶段：复制到 file-server 模块，暴露 REST/gRPC 接口</li>
 * <li>配置通过 {@link BitifulProperties} 注入，支持 ConfigMap/Secret</li>
 * </ul>
 * </p>
 *
 * @author Your Name
 * @since 1.0-SNAPSHOT
 */
@Service("BITIFUL")
@RequiredArgsConstructor
public class BitifulStorage implements FileStorageStrategy {

    private static final Logger log = LoggerFactory.getLogger(BitifulStorage.class);

    /** S3 客户端（由 BitifulConfig 注入） */
    private final S3Client s3Client;

    /** 预签名 URL 生成器 */
    private final S3Presigner s3Presigner;

    /** Bitiful 配置属性 */
    private final BitifulProperties properties;

    /** 日期格式化器：yyyy/MM/dd */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter
            .ofPattern(FileStorageConstants.DATE_PATH_PATTERN);

    /**
     * 上传文件到 Bitiful
     * <p>
     * 使用 {@link PutObjectRequest} 同步上传，支持任意大小文件。
     * 上传成功后生成 7 天有效期的签名访问 URL（GET）。
     * </p>
     * <p>
     * <strong>流程：</strong>
     * <ol>
     * <li>校验 fileKey（非空）</li>
     * <li>校验文件扩展名（白名单）</li>
     * <li>构建 PutObjectRequest（含 Content-Type）</li>
     * <li>执行 putObject</li>
     * <li>生成 7 天签名 URL 返回</li>
     * </ol>
     * </p>
     *
     * @param file    MultipartFile 文件（从 Controller 传入）
     * @param fileKey 存储键（业务层生成，如 "uploads/2025/11/xxx.jpg"）
     * @return 7 天有效期的访问 URL（可直链）
     * @throws BusinessException 上传失败（限额、权限、网络等）
     */
    @Override
    public String upload(MultipartFile file, String fileKey) {
        // 1. 校验 fileKey
        validateFileKey(fileKey);

        // 2. 校验文件大小
        validateFileSize(file);

        // 3. 校验扩展名（白名单）
        String originalFilename = file.getOriginalFilename();
        String extension = FilenameUtils.getExtension(originalFilename);
        validateFileExtension(extension);

        try {
            // 4. 构建 PutObjectRequest
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(fileKey)
                    .contentType(file.getContentType())
                    .build();

            // 5. 执行上传（流式，支持大文件）
            s3Client.putObject(putObjectRequest,
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            // 6. 记录成功日志
            log.info(FileStorageConstants.LOG_UPLOAD_SUCCESS,
                    properties.getBucket(), fileKey, file.getSize(), file.getContentType());

            // 7. 生成 7 天签名访问 URL
            return generateGetUrl(fileKey, Duration.ofDays(
                    FileStorageConstants.DEFAULT_PRESIGNED_GET_URL_EXPIRY_DAYS));

        } catch (S3Exception e) {
            log.error(FileStorageConstants.LOG_UPLOAD_FAILED, fileKey, e.getMessage(), e);
            String errorMsg = e.awsErrorDetails() != null
                    ? e.awsErrorDetails().errorMessage()
                    : e.getMessage();
            throw new BusinessException(FileErrorCode.FILE_STORAGE_ERROR,
                    "Bitiful 上传失败: " + errorMsg);
        } catch (Exception e) {
            log.error(FileStorageConstants.LOG_UPLOAD_FAILED, fileKey, e.getMessage(), e);
            throw new BusinessException(FileErrorCode.FILE_UPLOAD_FAILED,
                    "上传过程异常: " + e.getMessage());
        }
    }

    /**
     * 生成预签名 PUT URL（前端直传）
     * <p>
     * 用于前端直接上传到 Bitiful，绕过后端。
     * 过期时间由调用方控制（推荐 15~30 分钟）。
     * </p>
     * <p>
     * <strong>安全设计：</strong>
     * <ul>
     * <li>子账户最小权限（只允许 PUT 本 key）</li>
     * <li>URL 短效</li>
     * <li>上传后需回调后端完成入库</li>
     * </ul>
     * </p>
     *
     * @param fileKey       存储键（业务层生成）
     * @param expireMinutes 过期分钟数
     * @return 预签名 PUT URL
     * @throws BusinessException 生成失败
     */
    @Override
    public String generatePresignedUrl(String fileKey, int expireMinutes) {
        validateFileKey(fileKey);
        validateExpireMinutes(expireMinutes);

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(fileKey)
                    .contentType("application/octet-stream") // 前端可覆盖
                    .build();

            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(expireMinutes))
                    .putObjectRequest(putObjectRequest)
                    .build();

            PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);
            String url = presignedRequest.url().toString();

            log.info(FileStorageConstants.LOG_PRESIGNED_URL_SUCCESS, fileKey, expireMinutes);
            return url;

        } catch (Exception e) {
            log.error(FileStorageConstants.LOG_PRESIGNED_URL_FAILED, fileKey, e.getMessage(), e);
            throw new BusinessException(FileErrorCode.FILE_PRESIGNED_URL_FAILED,
                    "预签名 URL 生成失败: " + e.getMessage());
        }
    }

    /**
     * 获取 Bucket 名称
     *
     * @return bucket 名
     */
    @Override
    public String getBucketName() {
        return properties.getBucket();
    }

    /**
     * 删除文件
     * <p>
     * 软删除后调用，清理存储空间。
     * 失败时记录日志，不抛异常（非致命）。
     * </p>
     *
     * @param fileKey 存储键
     */
    @Override
    public void delete(String fileKey) {
        if (StringUtils.isBlank(fileKey)) {
            return;
        }

        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(fileKey)
                    .build());
            log.info(FileStorageConstants.LOG_DELETE_SUCCESS, fileKey);
        } catch (Exception e) {
            // 非致命，记录日志
            log.warn(FileStorageConstants.LOG_DELETE_FAILED, fileKey, e.getMessage());
        }
    }

    // ────────────────────────── 私有工具方法 ──────────────────────────

    /**
     * 生成签名 GET URL（上传后返回）
     *
     * @param fileKey  键
     * @param duration 有效时长
     * @return 签名 URL
     */
    private String generateGetUrl(String fileKey, Duration duration) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(fileKey)
                .build();

        var presignRequest = software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest.builder()
                .signatureDuration(duration)
                .getObjectRequest(getObjectRequest)
                .build();

        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    // ────────────────────────── 私有验证方法 ──────────────────────────

    /**
     * 校验 fileKey
     *
     * @param fileKey 存储键
     * @throws BusinessException 如果 fileKey 为空
     */
    private void validateFileKey(String fileKey) {
        if (StringUtils.isBlank(fileKey)) {
            throw new BusinessException(
                    FileErrorCode.FILE_UPLOAD_FAILED,
                    FileStorageConstants.ERROR_FILE_KEY_BLANK);
        }
    }

    /**
     * 校验文件大小
     *
     * @param file 文件
     * @throws BusinessException 如果文件大小超过限制
     */
    private void validateFileSize(MultipartFile file) {
        long fileSize = file.getSize();
        long maxSize = properties.getMaxFileSize();

        if (fileSize > maxSize) {
            log.warn(FileStorageConstants.LOG_FILE_SIZE_EXCEEDED,
                    fileSize, maxSize, file.getOriginalFilename());
            throw new BusinessException(
                    FileErrorCode.FILE_EXCEED_MAX_SIZE,
                    String.format(FileStorageConstants.ERROR_FILE_SIZE_EXCEEDED, fileSize, maxSize));
        }
    }

    /**
     * 校验文件扩展名（白名单）
     *
     * @param extension 扩展名（不含 .）
     * @throws BusinessException 如果扩展名不在白名单中
     */
    private void validateFileExtension(String extension) {
        if (StringUtils.isBlank(extension)) {
            throw new BusinessException(
                    FileErrorCode.FILE_INVALID_TYPE,
                    "文件扩展名不能为空");
        }

        String lowerExtension = extension.toLowerCase();
        if (!properties.getAllowedExtensions().contains(lowerExtension)) {
            log.warn(FileStorageConstants.LOG_INVALID_FILE_TYPE,
                    extension, properties.getAllowedExtensions());
            throw new BusinessException(
                    FileErrorCode.FILE_INVALID_TYPE,
                    String.format(FileStorageConstants.ERROR_INVALID_FILE_TYPE,
                            extension, String.join(", ", properties.getAllowedExtensions())));
        }
    }

    /**
     * 校验过期时间
     *
     * @param expireMinutes 过期分钟数
     * @throws BusinessException 如果过期时间不在有效范围
     */
    private void validateExpireMinutes(int expireMinutes) {
        if (expireMinutes < FileStorageConstants.MIN_PRESIGNED_URL_EXPIRY_MINUTES
                || expireMinutes > FileStorageConstants.MAX_PRESIGNED_URL_EXPIRY_MINUTES) {
            throw new BusinessException(
                    FileErrorCode.FILE_PRESIGNED_URL_FAILED,
                    String.format(FileStorageConstants.ERROR_INVALID_EXPIRY_TIME,
                            FileStorageConstants.MIN_PRESIGNED_URL_EXPIRY_MINUTES,
                            FileStorageConstants.MAX_PRESIGNED_URL_EXPIRY_MINUTES));
        }
    }

    /**
     * 生成标准 fileKey（业务层调用）
     * <p>
     * 示例：uploads/2025/11/25/abc123def456.jpg
     * </p>
     *
     * @param originalFilename 原始文件名
     * @return fileKey
     * @throws IllegalArgumentException 如果文件名为空
     */
    public static String generateFileKey(String originalFilename) {
        if (StringUtils.isBlank(originalFilename)) {
            throw new IllegalArgumentException("原始文件名不能为空");
        }

        String ext = FilenameUtils.getExtension(originalFilename);
        String datePath = Instant.now()
                .atZone(ZoneId.systemDefault())
                .format(DATE_FORMATTER);
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return String.format("%s%s/%s.%s",
                FileStorageConstants.UPLOAD_PATH_PREFIX, datePath, uuid, ext);
    }
}
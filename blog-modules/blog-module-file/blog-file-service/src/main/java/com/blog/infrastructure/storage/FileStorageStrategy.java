package com.blog.infrastructure.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件存储策略统一抽象
 * <p>
 * <strong>设计模式：</strong>
 * <ul>
 *   <li><strong>策略模式（Strategy）</strong>：统一对外接口，内部可切换不同实现</li>
 *   <li><strong>适配器模式（Adapter）</strong>：把第三方 SDK（Bitiful、阿里云 OSS、MinIO、本地磁盘）统一包装</li>
 *   <li><strong>依赖倒置（DIP）</strong>：业务层（FileService）只依赖此抽象，不依赖具体实现</li>
 * </ul>
 * </p>
 * <p>
 * <strong>作用：</strong>
 * <ul>
 *   <li>解耦业务与底层存储</li>
 *   <li>支持运行时切换（application.yml 中 <code>oss.type</code>）</li>
 *   <li>后期微服务拆分时，整个实现可直接搬到 <code>file-server</code></li>
 * </ul>
 * </p>
 *
 * @author Your Name
 * @since 1.0-SNAPSHOT
 */
public interface FileStorageStrategy {

    /**
     * 上传文件
     *
     * @param file    待上传的 MultipartFile（来自 Controller）
     * @param fileKey 存储键（业务层生成，形如 <code>uploads/2025/11/xxx.jpg</code>）
     * @return 访问 URL（7 天签名或公开读 URL）
     */
    String upload(MultipartFile file, String fileKey);

    /**
     * 生成预签名 PUT URL（前端直传）
     *
     * @param fileKey       存储键
     * @param expireMinutes 过期分钟数（推荐 15~30）
     * @return 预签名 URL
     */
    String generatePresignedUrl(String fileKey, int expireMinutes);

    /**
     * 获取 Bucket / 存储根目录名称（用于日志、监控）
     *
     * @return bucket 名或本地根路径
     */
    String getBucketName();

    /**
     * 删除文件（软删除后调用）
     *
     * @param fileKey 存储键
     */
    void delete(String fileKey);
}
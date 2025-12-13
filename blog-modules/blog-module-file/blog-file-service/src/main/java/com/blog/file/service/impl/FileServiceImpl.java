package com.blog.file.service.impl;

import com.blog.common.base.BaseServiceImpl;
import com.blog.common.exception.BusinessException;
import com.blog.dto.FileDTO;
import com.blog.dto.PreSignedUrlRequest;
import com.blog.enums.FileErrorCode;
import com.blog.file.converter.FileConverter;
import com.blog.file.entity.FileFile;
import com.blog.file.mapper.FileMapper;
import com.blog.infrastructure.storage.FileStorageStrategy;
import com.blog.infrastructure.storage.StorageContext;
import com.blog.service.IFileService;
import com.blog.vo.FileVO;
import com.blog.vo.PreSignedUploadVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

/**
 * 文件服务实现
 *
 * <p>
 * 遵循项目规范：
 * <ul>
 * <li>继承 BaseServiceImpl 获得CRUD能力</li>
 * <li>使用 @RequiredArgsConstructor 构造注入</li>
 * <li>返回 Result&lt;T&gt; 统一响应</li>
 * <li>抛出 BusinessException 处理异常</li>
 * </ul>
 * </p>
 *
 * <p>
 * <strong>核心功能</strong>：
 * <ul>
 * <li>生成预签名上传URL（官方推荐）</li>
 * <li>确认上传完成</li>
 * <li>动态生成访问URL</li>
 * <li>文件查询和删除</li>
 * </ul>
 * </p>
 *
 * @author liusxml
 * @since 1.0.0
 */
@Slf4j
@Service
public class FileServiceImpl
        extends BaseServiceImpl<FileMapper, FileFile, FileVO, FileDTO, FileConverter>
        implements IFileService {

    private final StorageContext storageContext;
    private final FileConverter converter;

    /**
     * 调用父类构造函数注入 converter
     */
    public FileServiceImpl(StorageContext storageContext, FileConverter converter) {
        super(converter);
        this.storageContext = storageContext;
        this.converter = converter;
    }

    // BaseServiceImpl 已注入：
    // - baseMapper (FileMapper)
    // - converter (FileConverter)

    /**
     * 生成预签名上传 URL（官方推荐方案）
     *
     * <p>
     * 流程：
     * <ol>
     * <li>生成唯一 fileKey</li>
     * <li>预创建文件记录（状态=待上传）</li>
     * <li>获取存储策略</li>
     * <li>生成预签名 PUT URL</li>
     * <li>返回给前端</li>
     * </ol>
     * </p>
     *
     * @param request 预签名 URL 请求
     * @return 预签名上传 VO
     */
    @Override
    public PreSignedUploadVO generateUploadUrl(PreSignedUrlRequest request) {
        log.info("生成预签名URL: fileName={}, size={}",
                request.getFileName(), request.getFileSize());

        // 秒传检测：如果提供了MD5，检查是否已存在相同文件
        if (request.getMd5() != null && !request.getMd5().isBlank()) {
            Optional<FileFile> existingFile = checkInstantUpload(request.getMd5(), request.getFileSize());
            if (existingFile.isPresent()) {
                log.info("秒传命中: md5={}, fileId={}", request.getMd5(), existingFile.get().getId());
                return createInstantUploadResponse(existingFile.get());
            }
        }

        // 1. 生成唯一文件键
        String fileKey = generateFileKey(request.getFileName());

        // 2. 提取文件信息
        String extension = extractExtension(request.getFileName());
        String fileCategory = detectFileCategory(request.getContentType());

        // 3. 预创建文件记录
        FileFile file = new FileFile();
        file.setFileKey(fileKey);
        file.setStorageType("BITIFUL"); // 当前使用 BITIFUL
        file.setOriginalName(request.getFileName());
        file.setFileSize(request.getFileSize());
        file.setContentType(request.getContentType());
        file.setExtension(extension);
        file.setMd5(request.getMd5());
        file.setFileCategory(fileCategory);
        file.setAccessPolicy("PRIVATE"); // 默认私有
        file.setUploadStatus(0); // 待上传
        // createBy 将由 MyBatis-Plus 自动填充

        // 使用 BaseServiceImpl 的 save 方法（自动填充 createTime 等）
        save(file);

        log.info("预创建文件记录: fileId={}, fileKey={}", file.getId(), fileKey);

        // 4. 获取存储策略并生成预签名 URL
        FileStorageStrategy strategy = storageContext.getStrategy("BITIFUL");
        int expireMinutes = request.getExpireMinutes();
        String uploadUrl = strategy.generatePresignedUrl(fileKey, expireMinutes);

        // 5. 构建响应
        PreSignedUploadVO vo = new PreSignedUploadVO();
        vo.setUploadUrl(uploadUrl);
        vo.setFileKey(fileKey);
        vo.setFileId(file.getId());
        vo.setExpireSeconds(expireMinutes * 60);
        vo.setCallbackUrl("/api/v1/files/" + file.getId() + "/confirm");

        log.info("生成预签名URL成功: fileId={}, expireMinutes={}",
                file.getId(), expireMinutes);

        return vo;
    }

    /**
     * 确认上传完成（前端回调）
     *
     * <p>
     * 前端上传成功后调用此接口，更新文件状态为"已完成"，
     * 并生成7天有效的访问URL。
     * </p>
     *
     * @param fileId 文件 ID
     */
    @Override
    public void confirmUpload(Long fileId) {
        log.info("确认上传: fileId={}", fileId);

        // 使用 BaseServiceImpl 的 getById
        FileFile file = getById(fileId);
        if (file == null) {
            throw new BusinessException(FileErrorCode.FILE_NOT_FOUND);
        }

        if (file.getUploadStatus() == 1) {
            log.warn("文件已确认，跳过: fileId={}", fileId);
            return;
        }

        // 更新状态
        file.setUploadStatus(1); // 已完成
        file.setUploadCompleteTime(LocalDateTime.now());
        // updateBy 将由 MyBatis-Plus 自动填充

        // 使用 BaseServiceImpl 的 updateById
        updateById(file);

        log.info("确认上传成功: fileId={}, fileKey={}", fileId, file.getFileKey());
    }

    /**
     * 动态生成访问 URL
     *
     * <p>
     * 不再存储 accessUrl，而是动态生成。
     * </p>
     *
     * @param fileId        文件 ID
     * @param expireMinutes 过期时间（分钟）
     * @return 访问 URL
     */
    public String getAccessUrl(Long fileId, int expireMinutes) {
        FileFile file = getById(fileId);
        if (file == null) {
            throw new BusinessException(FileErrorCode.FILE_NOT_FOUND);
        }

        FileStorageStrategy strategy = storageContext.getStrategy(file.getStorageType());
        // 使用 generateDownloadUrl 生成下载链接（GET），而不是 generatePresignedUrl（PUT）
        return strategy.generateDownloadUrl(file.getFileKey(), expireMinutes);
    }

    /**
     * 根据 ID 获取文件 VO
     *
     * @param id 文件 ID
     * @return 文件 VO（Optional）
     */
    @Override
    public Optional<FileVO> getVoById(Long id) {
        // BaseServiceImpl 已提供此方法，无需重写
        return super.getVoById(id);
    }

    /**
     * 根据 ID 删除文件
     *
     * <p>
     * 软删除 + 调用存储删除。
     * </p>
     *
     * @param id 文件 ID
     * @return 是否成功
     */
    @Override
    public boolean deleteById(Long id) {
        FileFile file = getById(id);
        if (file == null) {
            return false;
        }

        // 调用存储删除
        try {
            FileStorageStrategy strategy = storageContext.getStrategy(file.getStorageType());
            strategy.delete(file.getFileKey());
            log.info("删除存储文件成功: fileKey={}", file.getFileKey());
        } catch (Exception e) {
            log.error("删除存储文件失败: fileKey={}", file.getFileKey(), e);
            // 继续软删除
        }

        // 软删除（BaseServiceImpl 的 removeById）
        return removeById(id);
    }

    // ========== 私有工具方法 ==========

    /**
     * 生成文件键
     *
     * <p>
     * 格式：uploads/YYYY/MM/DD/UUID.ext
     * </p>
     */
    private String generateFileKey(String originalName) {
        String datePrefix = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String extension = extractExtension(originalName);
        String uuid = UUID.randomUUID().toString().replace("-", "");

        return String.format("uploads/%s/%s.%s", datePrefix, uuid, extension);
    }

    /**
     * 提取文件扩展名
     */
    private String extractExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }

    /**
     * 检测文件分类
     */
    private String detectFileCategory(String contentType) {
        if (contentType == null) {
            return "OTHER";
        }

        if (contentType.startsWith("image/")) {
            return "IMAGE";
        } else if (contentType.startsWith("video/")) {
            return "VIDEO";
        } else if (contentType.startsWith("audio/")) {
            return "AUDIO";
        } else if (contentType.contains("pdf") || contentType.contains("document")) {
            return "DOCUMENT";
        }

        return "OTHER";
    }

    // ========== 秒传逻辑 ==========

    /**
     * 检查秒传：根据MD5和文件大小查找已存在的文件
     *
     * @param md5      文件MD5
     * @param fileSize 文件大小
     * @return 已存在的文件（Optional）
     */
    private Optional<FileFile> checkInstantUpload(String md5, Long fileSize) {
        return Optional.ofNullable(
                baseMapper.selectOne(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<FileFile>()
                                .eq(FileFile::getMd5, md5)
                                .eq(FileFile::getFileSize, fileSize)
                                .eq(FileFile::getUploadStatus, 1) // 只匹配已完成的文件
                                .last("LIMIT 1")));
    }

    /**
     * 创建秒传响应
     *
     * @param existingFile 已存在的文件
     * @return 预签名上传 VO（标记为秒传）
     */
    private PreSignedUploadVO createInstantUploadResponse(FileFile existingFile) {
        PreSignedUploadVO vo = new PreSignedUploadVO();
        vo.setUploadUrl(null); // 秒传不需要上传URL
        vo.setFileKey(existingFile.getFileKey());
        vo.setFileId(existingFile.getId());
        vo.setExpireSeconds(0); // 秒传无需过期时间
        vo.setCallbackUrl(null); // 秒传无需回调
        vo.setInstant(true); // 标记为秒传

        return vo;
    }
}

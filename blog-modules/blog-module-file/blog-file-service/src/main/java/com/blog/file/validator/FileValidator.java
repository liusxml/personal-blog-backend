package com.blog.file.validator;

import com.blog.common.config.BitifulProperties;
import com.blog.common.exception.BusinessException;
import com.blog.common.exception.SystemErrorCode;
import com.blog.file.api.dto.PreSignedUrlRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 文件验证器
 *
 * <p>
 * 验证文件上传的业务规则：
 * <ul>
 * <li>文件大小限制</li>
 * <li>文件扩展名白名单</li>
 * <li>Content-Type 验证</li>
 * </ul>
 * </p>
 *
 * @author liusxml
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
public class FileValidator {

    private final BitifulProperties bitifulProperties;

    /**
     * 验证预签名上传请求
     *
     * @param request 请求对象
     * @throws BusinessException 验证失败时抛出
     */
    public void validateUploadRequest(PreSignedUrlRequest request) {
        // 验证文件大小
        validateFileSize(request.getFileSize());

        // 验证文件扩展名
        validateExtension(request.getFileName());

        // 验证 Content-Type
        validateContentType(request.getContentType());
    }

    /**
     * 验证文件大小
     */
    private void validateFileSize(Long fileSize) {
        if (fileSize == null || fileSize <= 0) {
            throw new BusinessException(SystemErrorCode.PARAM_ERROR,
                    "文件大小无效");
        }

        Long maxSize = bitifulProperties.getMaxFileSize();
        if (fileSize > maxSize) {
            throw new BusinessException(SystemErrorCode.PARAM_ERROR,
                    String.format("文件大小超过限制（%d MB）", maxSize / 1024 / 1024));
        }
    }

    /**
     * 验证文件扩展名
     */
    private void validateExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            throw new BusinessException(SystemErrorCode.PARAM_ERROR,
                    "文件名无效");
        }

        String extension = extractExtension(fileName);
        Set<String> allowedExtensions = bitifulProperties.getAllowedExtensions();

        if (!allowedExtensions.contains(extension)) {
            throw new BusinessException(SystemErrorCode.PARAM_ERROR,
                    String.format("不支持的文件类型：%s。允许的类型：%s",
                            extension, allowedExtensions));
        }
    }

    /**
     * 验证 Content-Type
     */
    private void validateContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            throw new BusinessException(SystemErrorCode.PARAM_ERROR,
                    "Content-Type 不能为空");
        }

        // 基本验证格式
        if (!contentType.contains("/")) {
            throw new BusinessException(SystemErrorCode.PARAM_ERROR,
                    "Content-Type 格式无效");
        }
    }

    /**
     * 提取文件扩展名
     */
    private String extractExtension(String fileName) {
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }
}

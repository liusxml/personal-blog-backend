package com.blog.file;

import com.blog.common.config.BitifulProperties;
import com.blog.common.exception.BusinessException;
import com.blog.enums.FileErrorCode;
import com.blog.infrastructure.oss.BitifulStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URI;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * BitifulStorage 单元测试
 * <p>
 * 使用 Mockito 进行纯单元测试，不启动 Spring 容器。
 * 遵循测试金字塔理论，提供快速、隔离的测试反馈。
 * </p>
 *
 * <p>
 * <strong>测试策略：</strong>
 * </p>
 * <ul>
 * <li>使用 @Mock 模拟所有外部依赖（S3Client, S3Presigner, Properties）</li>
 * <li>使用 @InjectMocks 自动注入 Mock 到被测试类</li>
 * <li>每个测试方法独立，互不影响</li>
 * <li>覆盖成功场景、异常场景、边界条件</li>
 * </ul>
 *
 * <p>
 * <strong>为什么使用单元测试：</strong>
 * </p>
 * <ul>
 * <li>✅ 执行速度快（毫秒级）</li>
 * <li>✅ 完全隔离，不依赖外部服务</li>
 * <li>✅ 易于调试和维护</li>
 * <li>✅ 满足测试金字塔理论（70% 单元测试）</li>
 * </ul>
 *
 * @author liusxml
 * @since 1.0-SNAPSHOT
 */
@ExtendWith(MockitoExtension.class) // 使用 Mockito 扩展，不启动 Spring
@DisplayName("BitifulStorage 单元测试")
class BitifulStorageUnitTest {

        @Mock
        private S3Client s3Client; // Mock S3 客户端

        @Mock
        private S3Presigner s3Presigner; // Mock 预签名生成器

        @Mock
        private BitifulProperties properties; // Mock 配置属性

        @InjectMocks
        private BitifulStorage bitifulStorage; // 自动注入上述 Mock

        @BeforeEach
        void setUp() {
                // 配置 Mock 的默认行为（使用 lenient 避免 UnnecessaryStubbingException）
                lenient().when(properties.getBucket()).thenReturn("test-bucket");
                lenient().when(properties.getMaxFileSize()).thenReturn(10 * 1024 * 1024L); // 10MB
                lenient().when(properties.getAllowedExtensions()).thenReturn(Set.of("jpg", "jpeg", "png", "pdf"));
        }

        // ==================== 文件上传测试 ====================

        @Test
        @DisplayName("测试文件上传成功")
        void testUploadSuccess() throws Exception {
                // Given: 准备测试数据
                MockMultipartFile file = new MockMultipartFile(
                                "file", "test.jpg", "image/jpeg", "test data".getBytes());
                String fileKey = "uploads/2025/11/25/test.jpg";

                // Mock S3 客户端行为
                when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                                .thenReturn(PutObjectResponse.builder().build());

                // Mock 预签名 URL 生成
                PresignedGetObjectRequest mockPresigned = mock(PresignedGetObjectRequest.class);
                when(mockPresigned.url()).thenReturn(URI.create("https://test.url/file.jpg").toURL());
                when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                                .thenReturn(mockPresigned);

                // When: 执行上传
                String resultUrl = bitifulStorage.upload(file, fileKey);

                // Then: 验证结果
                assertThat(resultUrl).isEqualTo("https://test.url/file.jpg");

                // 验证 S3 客户端被正确调用
                ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
                verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));

                PutObjectRequest capturedRequest = requestCaptor.getValue();
                assertThat(capturedRequest.bucket()).isEqualTo("test-bucket");
                assertThat(capturedRequest.key()).isEqualTo(fileKey);
                assertThat(capturedRequest.contentType()).isEqualTo("image/jpeg");
        }

        @Test
        @DisplayName("测试文件上传 - fileKey 为空抛出异常")
        void testUploadWithBlankFileKey() {
                // Given
                MockMultipartFile file = new MockMultipartFile(
                                "file", "test.jpg", "image/jpeg", "data".getBytes());

                // When & Then
                assertThatThrownBy(() -> bitifulStorage.upload(file, ""))
                                .isInstanceOf(BusinessException.class)
                                .hasMessageContaining("fileKey 不能为空");

                // 验证没有调用 S3 客户端
                verifyNoInteractions(s3Client);
        }

        @Test
        @DisplayName("测试文件上传 - 文件大小超限")
        void testUploadWithFileSizeExceeded() {
                // Given: 创建 11MB 文件（超过 10MB 限制）
                byte[] largeData = new byte[11 * 1024 * 1024];
                MockMultipartFile file = new MockMultipartFile(
                                "file", "large.jpg", "image/jpeg", largeData);

                // When & Then
                assertThatThrownBy(() -> bitifulStorage.upload(file, "uploads/large.jpg"))
                                .isInstanceOf(BusinessException.class)
                                .extracting("errorCode.code")
                                .isEqualTo(FileErrorCode.FILE_EXCEED_MAX_SIZE.getCode());

                // 验证没有调用 S3 客户端
                verifyNoInteractions(s3Client);
        }

        @Test
        @DisplayName("测试文件上传 - 无效文件类型")
        void testUploadWithInvalidFileType() {
                // Given
                MockMultipartFile file = new MockMultipartFile(
                                "file", "malware.exe", "application/exe", "data".getBytes());

                // When & Then
                assertThatThrownBy(() -> bitifulStorage.upload(file, "uploads/test.exe"))
                                .isInstanceOf(BusinessException.class)
                                .hasMessageContaining("不支持的文件类型");

                // 验证没有调用 S3 客户端
                verifyNoInteractions(s3Client);
        }

        @Test
        @DisplayName("测试文件上传 - S3 异常处理")
        void testUploadWithS3Exception() {
                // Given
                MockMultipartFile file = new MockMultipartFile(
                                "file", "test.jpg", "image/jpeg", "data".getBytes());

                // Mock S3 抛出异常（S3Exception 需要特殊处理）
                when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                                .thenThrow(S3Exception.builder()
                                                .message("Access Denied")
                                                .statusCode(403)
                                                .build());

                // When & Then
                assertThatThrownBy(() -> bitifulStorage.upload(file, "uploads/test.jpg"))
                                .isInstanceOf(BusinessException.class)
                                .hasMessageContaining("Bitiful 上传失败");
        }

        // ==================== 预签名 URL 测试 ====================

        @Test
        @DisplayName("测试预签名 URL 生成成功")
        void testGeneratePresignedUrlSuccess() throws Exception {
                // Given
                String fileKey = "uploads/test.jpg";
                int expireMinutes = 30;

                // Mock 预签名生成
                PresignedPutObjectRequest mockPresigned = mock(PresignedPutObjectRequest.class);
                when(mockPresigned.url()).thenReturn(URI.create("https://presigned.url").toURL());
                when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class)))
                                .thenReturn(mockPresigned);

                // When
                String url = bitifulStorage.generatePresignedUrl(fileKey, expireMinutes);

                // Then
                assertThat(url).isEqualTo("https://presigned.url");
                verify(s3Presigner).presignPutObject(any(PutObjectPresignRequest.class));
        }

        @Test
        @DisplayName("测试预签名 URL - fileKey 为空")
        void testGeneratePresignedUrlWithBlankFileKey() {
                // When & Then
                assertThatThrownBy(() -> bitifulStorage.generatePresignedUrl("", 30))
                                .isInstanceOf(BusinessException.class)
                                .hasMessageContaining("fileKey 不能为空");
        }

        @Test
        @DisplayName("测试预签名 URL - 过期时间无效（小于最小值）")
        void testGeneratePresignedUrlWithInvalidExpireTime() {
                // When & Then: 过期时间为 0（小于 1 分钟）
                assertThatThrownBy(() -> bitifulStorage.generatePresignedUrl("test.jpg", 0))
                                .isInstanceOf(BusinessException.class)
                                .hasMessageContaining("过期时间应在");
        }

        @Test
        @DisplayName("测试预签名 URL - 过期时间无效（大于最大值）")
        void testGeneratePresignedUrlWithExpireTimeTooLarge() {
                // When & Then: 过期时间为 61 分钟（大于 60 分钟）
                assertThatThrownBy(() -> bitifulStorage.generatePresignedUrl("test.jpg", 61))
                                .isInstanceOf(BusinessException.class)
                                .hasMessageContaining("过期时间应在");
        }

        // ==================== 文件删除测试 ====================

        @Test
        @DisplayName("测试文件删除成功")
        void testDeleteSuccess() {
                // Given
                String fileKey = "uploads/test.jpg";

                // When
                bitifulStorage.delete(fileKey);

                // Then: 验证删除请求被调用
                ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
                verify(s3Client).deleteObject(captor.capture());

                DeleteObjectRequest request = captor.getValue();
                assertThat(request.bucket()).isEqualTo("test-bucket");
                assertThat(request.key()).isEqualTo(fileKey);
        }

        @Test
        @DisplayName("测试文件删除 - fileKey 为空不抛异常")
        void testDeleteWithBlankFileKey() {
                // Given & When
                bitifulStorage.delete("");
                bitifulStorage.delete(null);

                // Then: 不应该调用 S3 客户端
                verifyNoInteractions(s3Client);
        }

        @Test
        @DisplayName("测试文件删除 - S3 异常不影响流程")
        void testDeleteWithS3Exception() {
                // Given
                when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                                .thenThrow(S3Exception.builder().message("Not Found").build());

                // When & Then: 删除失败不应抛出异常（只记录日志）
                bitifulStorage.delete("uploads/test.jpg");

                // 验证仍然调用了删除
                verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
        }

        // ==================== getBucketName 测试 ====================

        @Test
        @DisplayName("测试获取 Bucket 名称")
        void testGetBucketName() {
                // When
                String bucketName = bitifulStorage.getBucketName();

                // Then
                assertThat(bucketName).isEqualTo("test-bucket");
        }

        // ==================== generateFileKey 静态方法测试 ====================

        @Test
        @DisplayName("测试 generateFileKey - 正常生成")
        void testGenerateFileKey() {
                // When
                String fileKey = BitifulStorage.generateFileKey("test.jpg");

                // Then
                assertThat(fileKey)
                                .startsWith("uploads/")
                                .contains("/")
                                .endsWith(".jpg");
        }

        @Test
        @DisplayName("测试 generateFileKey - 文件名为空抛异常")
        void testGenerateFileKeyWithBlankFilename() {
                // When & Then
                assertThatThrownBy(() -> BitifulStorage.generateFileKey(""))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("原始文件名不能为空");

                assertThatThrownBy(() -> BitifulStorage.generateFileKey(null))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("原始文件名不能为空");
        }
}

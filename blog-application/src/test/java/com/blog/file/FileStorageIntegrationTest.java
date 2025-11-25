package com.blog.file;

import com.blog.BlogApplication;
import com.blog.common.exception.BusinessException;
import com.blog.infrastructure.config.BitifulProperties;
import com.blog.infrastructure.oss.BitifulStorage;
import com.blog.infrastructure.storage.FileStorageStrategy;
import com.blog.infrastructure.storage.StorageContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 文件存储集成测试
 * <p>
 * 使用 @SpringBootTest 启动完整 Spring 容器，验证配置加载和 Bean 装配。
 * 使用 @MockBean 替换外部依赖（S3Client, S3Presigner），避免真实网络调用。
 * </p>
 *
 * <p>
 * <strong>测试策略：</strong>
 * </p>
 * <ul>
 * <li>验证 Spring Boot 配置正确加载（@ConfigurationProperties）</li>
 * <li>验证 Bean 正确装配和依赖注入</li>
 * <li>验证策略模式工厂正常工作</li>
 * <li>端到端验证一个完整场景</li>
 * </ul>
 *
 * <p>
 * <strong>为什么使用集成测试：</strong>
 * </p>
 * <ul>
 * <li>✅ 验证 Spring 配置正确性</li>
 * <li>✅ 验证 Bean 装配无误</li>
 * <li>✅ 发现配置问题（如缺少 @Component）</li>
 * <li>✅ 满足测试金字塔理论（20% 集成测试）</li>
 * </ul>
 *
 * <p>
 * <strong>@MockBean vs @Mock：</strong>
 * </p>
 * <ul>
 * <li>@MockBean: 替换 Spring 容器中的 Bean</li>
 * <li>@Mock: Mockito 纯 Mock，不涉及 Spring</li>
 * </ul>
 *
 * @author liusxml
 * @since 1.0-SNAPSHOT
 */
@SpringBootTest(classes = BlogApplication.class)
@ActiveProfiles("test")
@DisplayName("文件存储集成测试")
class FileStorageIntegrationTest {

    // ==================== 使用 @MockBean 替换 Spring 容器中的 Bean ====================
    // 注意: @MockBean 在 Spring Boot 3.4.0 被标记为 deprecated
    // 官方推荐替代方案: @MockitoBean (Spring Boot 3.4.0+)
    // 当前使用 @MockBean 原因:
    // 1. Spring Boot 3.5.7 中 @MockitoBean 可能尚未完全实现
    // 2. @MockBean 在 Spring Boot 4.0.0 之前仍然可用
    // 3. 功能完全正常，只是有 deprecation warning
    // TODO: 升级到 Spring Boot 4.0+ 时迁移到 @MockitoBean

    @SuppressWarnings("deprecation")
    @MockBean
    private S3Client s3Client; // 替换真实的 S3Client Bean

    @SuppressWarnings("deprecation")
    @MockBean
    private S3Presigner s3Presigner; // 替换真实的 S3Presigner Bean

    // ==================== 使用 @Autowired 注入真实的 Spring Bean ====================

    @Autowired
    private BitifulProperties bitifulProperties; // 真实配置（从 application-test.yaml 加载）

    @Autowired
    private StorageContext storageContext; // 真实策略工厂

    @Autowired
    private BitifulStorage bitifulStorage; // 真实存储实现（但依赖的 S3Client 是 Mock）

    // ==================== 配置加载测试 ====================

    @Test
    @DisplayName("测试1: BitifulProperties 配置正确加载")
    void testBitifulPropertiesLoaded() {
        // 验证配置从 application-test.yaml 正确加载
        assertThat(bitifulProperties).isNotNull();
        assertThat(bitifulProperties.getEndpoint()).isEqualTo("https://s3.bitiful.net/");
        assertThat(bitifulProperties.getRegion()).isEqualTo("cn-east-1");
        assertThat(bitifulProperties.getBucket()).isEqualTo("blog-files-test");

        // 新增配置项验证
        assertThat(bitifulProperties.getMaxFileSize()).isEqualTo(10485760L); // 10MB
        assertThat(bitifulProperties.getAllowedExtensions())
                .contains("jpg", "jpeg", "png", "gif", "webp", "pdf", "docx");
    }

    // ==================== Bean 装配测试 ====================

    @Test
    @DisplayName("测试2: Spring Bean 正确装配")
    void testBeansWiredCorrectly() {
        // 验证所有 Bean 都被正确注入
        assertThat(s3Client).isNotNull();
        assertThat(s3Presigner).isNotNull();
        assertThat(storageContext).isNotNull();
        assertThat(bitifulStorage).isNotNull();

        // 验证 Storage 中的依赖也被正确注入
        assertThat(bitifulStorage.getBucketName()).isEqualTo("blog-files-test");
    }

    // ==================== 策略模式测试 ====================

    @Test
    @DisplayName("测试3: StorageContext 策略工厂正常工作")
    void testStorageContextStrategyPattern() {
        // 验证可以通过工厂获取正确的策略
        FileStorageStrategy strategy = storageContext.getStrategy("BITIFUL");

        assertThat(strategy).isNotNull();
        assertThat(strategy).isInstanceOf(BitifulStorage.class);
        assertThat(strategy.getBucketName()).isEqualTo("blog-files-test");
    }

    @Test
    @DisplayName("测试4: StorageContext 不支持的策略抛出异常")
    void testStorageContextUnsupportedStrategy() {
        // 验证不支持的存储类型会抛出正确的异常
        assertThatThrownBy(() -> storageContext.getStrategy("ALIYUN_OSS"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不支持的存储类型");
    }

    // ==================== 端到端场景测试 ====================

    @Test
    @DisplayName("测试5: 端到端文件上传流程（使用 MockBean）")
    void testEndToEndUploadFlow() throws Exception {
        // Given: Mock S3Client 的行为
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        // Mock S3Presigner 的行为
        PresignedGetObjectRequest mockPresigned = mock(PresignedGetObjectRequest.class);
        when(mockPresigned.url()).thenReturn(URI.create("https://test-url.com/file.jpg").toURL());
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                .thenReturn(mockPresigned);

        // When: 执行完整的上传流程
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", "test data".getBytes());

        String resultUrl = bitifulStorage.upload(file, "uploads/2025/11/25/test.jpg");

        // Then: 验证返回的 URL 正确
        assertThat(resultUrl).isEqualTo("https://test-url.com/file.jpg");
    }
}
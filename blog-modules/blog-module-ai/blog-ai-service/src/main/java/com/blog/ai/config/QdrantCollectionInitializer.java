package com.blog.ai.config;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections.Distance;
import io.qdrant.client.grpc.Collections.PayloadSchemaType;
import io.qdrant.client.grpc.Collections.VectorParams;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Qdrant Collection 初始化器
 *
 * <p>
 * 应用启动时自动检查 Qdrant Cloud 中的 {@code articles} Collection 是否存在，
 * 不存在则自动创建（幂等操作，多次启动安全）。
 * </p>
 *
 * <p>
 * 与 {@link QdrantEmbeddingStoreConfig} 分离的原因：
 * 避免在 {@code @Configuration} 类的 {@code @PostConstruct} 中调用 {@code @Bean} 方法，
 * 以规避 Spring CGLIB 代理的潜在问题（Bean 方法调用会绕过代理缓存）。
 * </p>
 *
 * <p>
 * 规范合规：
 * </p>
 * <ul>
 *   <li>使用 {@code @RequiredArgsConstructor} 构造注入（Rule 5.3）</li>
 *   <li>注入 {@link QdrantClient} 而非直接调用 Config 方法</li>
 * </ul>
 *
 * @author liusxml
 * @since 1.3.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QdrantCollectionInitializer {

    private final QdrantClient qdrantClient;

    @Value("${qdrant.collection-name:articles}")
    private String collectionName;

    @Value("${qdrant.vector-dimension:1024}")
    private int vectorDimension;

    /**
     * 启动时确保 Collection 存在
     *
     * <p>
     * 已存在则跳过，不存在则以 Cosine 相似度 + 1024 维度创建。
     * 任何异常均记录日志但不阻断启动（不抛出）。
     * </p>
     */
    @PostConstruct
    public void ensureCollectionExists() {
        try {
            boolean exists = qdrantClient.collectionExistsAsync(collectionName).get();
            if (exists) {
                log.info("Qdrant collection '{}' already exists, skip creation", collectionName);
            } else {
                log.info("Qdrant collection '{}' not found, creating (dim={}, distance=Cosine)...",
                        collectionName, vectorDimension);
                qdrantClient.createCollectionAsync(
                        collectionName,
                        VectorParams.newBuilder()
                                .setDistance(Distance.Cosine)
                                .setSize(vectorDimension)
                                .build()
                ).get();
                log.info("Qdrant collection '{}' created successfully", collectionName);
            }

            // 无论 Collection 是新建还是已存在，都确保 articleId 有 Payload Index
            // Qdrant Filter 按 payload 字段过滤时（如删除文章向量），必须有对应的索引
            ensurePayloadIndex();

        } catch (Exception e) {
            // 不阻断启动，仅记录日志。如果 Collection 不存在，后续 embed/search 操作会失败并记录错误
            log.error("Failed to initialize Qdrant collection '{}': {}",
                    collectionName, e.getMessage(), e);
        }
    }

    /**
     * 确保 articleId 字段存在 Payload Index
     *
     * <p>
     * Qdrant 要求：使用 Filter 按某个 payload 字段过滤时，该字段必须有索引。
     * 否则会报 "Index required but not found" 错误。
     * </p>
     *
     * <p>
     * 幂等操作：如果索引已存在，Qdrant 会返回成功（不会报错）。
     * </p>
     */
    private void ensurePayloadIndex() {
        try {
            qdrantClient.createPayloadIndexAsync(
                    collectionName,
                    "articleId",
                    PayloadSchemaType.Keyword,  // 字符串类型用 Keyword
                    null,                       // indexParams（使用默认值）
                    null,                       // wait（使用默认 true）
                    null,                       // ordering（使用默认值）
                    null                        // timeout（使用默认值）
            ).get();
            log.info("Qdrant payload index ensured for field 'articleId' in collection '{}'", collectionName);
        } catch (Exception e) {
            // 索引可能已存在（幂等操作），仅 warn 不影响启动
            log.warn("Failed to ensure payload index for 'articleId': {}", e.getMessage());
        }
    }
}

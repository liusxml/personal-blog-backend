package com.blog.ai.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Qdrant Cloud 向量数据库配置
 *
 * <p>
 * 提供两个 Spring Bean：
 * </p>
 * <ul>
 *   <li>{@link EmbeddingStore} — 供 {@code ArticleEmbeddingHandler}（写入）
 *       和 {@code RagServiceImpl}（检索）使用</li>
 *   <li>{@link QdrantClient} — 供 {@code QdrantCollectionInitializer} 管理 Collection 生命周期</li>
 * </ul>
 *
 * <p>
 * 连接说明：使用 gRPC 端口 6334 + TLS，适配 Qdrant Cloud 要求。
 * API Key 通过环境变量 {@code QDRANT_API_KEY} 注入，禁止硬编码。
 * </p>
 *
 * @author liusxml
 * @since 1.3.0
 */
@Slf4j
@Configuration
public class QdrantEmbeddingStoreConfig {

    @Value("${qdrant.host:7ff7ee79-22bd-4767-81d8-eab241031f89.us-east-1-1.aws.cloud.qdrant.io}")
    private String host;

    @Value("${qdrant.port:6334}")
    private int port;

    @Value("${qdrant.api-key}")
    private String apiKey;

    @Value("${qdrant.collection-name:articles}")
    private String collectionName;

    /**
     * Qdrant EmbeddingStore Bean
     *
     * <p>
     * 替代 {@code MySQLEmbeddingStore}，成为全局唯一的 {@code EmbeddingStore<TextSegment>} Bean。
     * 使用 TLS（Cloud 强制要求）和 API Key 鉴权。
     * </p>
     */
    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        log.info("Initializing QdrantEmbeddingStore: host={}, port={}, collection={}",
                host, port, collectionName);
        return QdrantEmbeddingStore.builder()
                .host(host)
                .port(port)
                .useTls(true)
                .apiKey(apiKey)
                .collectionName(collectionName)
                .build();
    }

    /**
     * Qdrant gRPC 客户端 Bean
     *
     * <p>
     * 使用官方推荐的 {@code QdrantGrpcClient.newBuilder(host, port, useTls)} 重载
     * （来自 Qdrant 官方 Quickstart 文档，比手动构建 ManagedChannel 更简洁）。
     * 供 {@code QdrantCollectionInitializer} 在启动时自动创建 Collection 使用。
     * </p>
     */
    @Bean
    public QdrantClient qdrantClient() {
        return new QdrantClient(
                QdrantGrpcClient.newBuilder(host, port, true)   // host, grpcPort, useTls=true
                        .withApiKey(apiKey)
                        .build());
    }
}

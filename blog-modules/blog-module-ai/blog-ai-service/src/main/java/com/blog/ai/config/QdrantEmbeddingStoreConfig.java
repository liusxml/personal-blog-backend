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

    @Value("${qdrant.host:6536a180-3042-4194-a009-afe5b7e50051.us-east-1-1.aws.cloud.qdrant.io}")
    private String host;

    @Value("${qdrant.port:6334}")
    private int port;

    @Value("${qdrant.api-key}")
    private String apiKey;

    @Value("${qdrant.collection-name:articles}")
    private String collectionName;

    /**
     * Qdrant gRPC 客户端 Bean（全局复用单条 gRPC Channel）
     *
     * <p>
     * 使用官方 {@code QdrantGrpcClient.newBuilder(host, port, useTls, checkCompatibility)} 重载：
     * <ul>
     *   <li>{@code useTransportLayerSecurity = true}：Qdrant Cloud 强制要求 TLS 加密连接</li>
     *   <li>{@code checkCompatibility = false}：跳过启动时的跨版本比对请求（消除 client 1.17 vs server 1.19 警告，并节省一次跨国 gRPC 往返耗时）</li>
     * </ul>
     * </p>
     */
    @Bean
    public QdrantClient qdrantClient() {
        return new QdrantClient(
                QdrantGrpcClient.newBuilder(host, port, true, false)
                        .withApiKey(apiKey)
                        .build());
    }

    /**
     * Qdrant EmbeddingStore Bean
     *
     * <p>
     * 直接复用上方的 {@link QdrantClient} Bean，避免重复建立第二条底层 Netty gRPC 连接池。
     * </p>
     */
    @Bean
    public EmbeddingStore<TextSegment> embeddingStore(QdrantClient qdrantClient) {
        log.info("Initializing QdrantEmbeddingStore: host={}, port={}, collection={}",
                host, port, collectionName);
        return QdrantEmbeddingStore.builder()
                .client(qdrantClient)
                .collectionName(collectionName)
                .build();
    }
}

package com.blog.ai.service.impl;

import com.blog.ai.api.service.TextEmbeddingService;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * 基于阿里云通义千问 text-embedding-v4 的向量化服务实现
 *
 * <p>
 * 通过 {@code langchain4j-community-dashscope-spring-boot-starter} 自动注入
 * {@link EmbeddingModel} Bean，无需手动构建客户端。
 * </p>
 *
 * <p>
 * 配置项（application.yml）：
 * <pre>
 * langchain4j:
 *   community:
 *     dashscope:
 *       embedding-model:
 *         api-key: ${DASHSCOPE_API_KEY}
 *         model-name: text-embedding-v4
 *         dimension: 1536
 * </pre>
 * </p>
 *
 * @author liusxml
 * @since 1.2.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashScopeTextEmbeddingService implements TextEmbeddingService {

    /** LangChain4j 自动装配的 DashScope Embedding 模型 */
    private final EmbeddingModel embeddingModel;

    /**
     * 将文本转换为 1536 维向量
     *
     * <p>输入超过 2000 字符会被截断，以避免超出 token 上限。</p>
     *
     * @param text 输入文本
     * @return 1536 维浮点数向量
     */
    @Override
    public float[] embed(String text) {
        // 截断防止超出 token 上限（Rule 5.4：使用 Lang3 StringUtils）
        String truncated = StringUtils.left(text, 2000);
        log.debug("Generating embedding, text length: {}", truncated.length());

        float[] vector = embeddingModel.embed(truncated).content().vector();
        log.debug("Embedding generated, dimension: {}", vector.length);
        return vector;
    }

    /**
     * 检查 Embedding 服务是否可用
     *
     * @return true 表示 EmbeddingModel Bean 已成功注入（API Key 已配置）
     */
    @Override
    public boolean isAvailable() {
        return embeddingModel != null;
    }
}

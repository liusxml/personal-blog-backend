package com.blog.article.infrastructure.vector;

/**
 * Embedding 服务接口
 *
 * <p>
 * 负责将文本内容转换为向量表示（Embedding）。
 * </p>
 *
 * <p>
 * 设计说明：
 * </p>
 * <ul>
 * <li>接口定义：支持多种Embedding API实现（OpenAI、DashScope、Ollama、Mock）</li>
 * <li>向量维度：1536（与MySQL VECTOR字段一致）</li>
 * <li>异步处理：实现类应支持异步调用，避免阻塞主业务</li>
 * <li>错误处理：API调用失败时应有降级策略</li>
 * </ul>
 *
 * @author liusxml
 * @since 1.1.0
 */
public interface EmbeddingService {

    /**
     * 向量维度（与MySQL VECTOR字段保持一致）
     */
    int VECTOR_DIMENSION = 1536;

    /**
     * 生成文本的向量表示
     *
     * <p>
     * 将输入文本转换为1536维的浮点数向量。
     * </p>
     *
     * @param text 输入文本（文章标题 + 内容摘要）
     * @return 1536维向量数组
     * @throws RuntimeException 如果API调用失败
     */
    float[] generateEmbedding(String text);

    /**
     * 异步生成并保存文章向量
     *
     * <p>
     * 从数据库加载文章内容，生成向量后更新到数据库。
     * </p>
     *
     * <p>
     * 典型场景：
     * </p>
     * <ul>
     * <li>文章发布时触发（通过ArticlePublishedEvent）</li>
     * <li>文章内容更新后重新生成</li>
     * </ul>
     *
     * @param articleId 文章ID
     */
    void generateAndSaveAsync(Long articleId);

    /**
     * 将浮点数组转换为MySQL VECTOR格式字符串
     *
     * <p>
     * 格式：'[0.1, 0.2, 0.3, ...]'
     * </p>
     *
     * @param vector 向量数组
     * @return VECTOR格式字符串
     */
    default String vectorToString(float[] vector) {
        if (vector == null || vector.length == 0) {
            return "[]";
        }

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0)
                sb.append(", ");
            sb.append(vector[i]);
        }
        sb.append("]");

        return sb.toString();
    }

    /**
     * 检查Embedding服务是否可用
     *
     * @return true 如果服务可用
     */
    boolean isAvailable();
}

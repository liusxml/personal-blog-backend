package com.blog.ai.api.service;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 文本向量化服务接口
 *
 * <p>
 * 纯 AI 能力抽象，只关心"把文字变成向量"，不感知任何业务领域概念（无 articleId）。
 * </p>
 *
 * <p>
 * 支持多种实现：
 * </p>
 * <ul>
 *   <li>DashScopeTextEmbeddingService — 通义 text-embedding-v4（生产）</li>
 *   <li>MockTextEmbeddingService — 随机向量（测试/降级）</li>
 * </ul>
 *
 * @author liusxml
 * @since 1.2.0
 */
public interface TextEmbeddingService {

    /** 向量维度，与 MySQL VECTOR(1536) 字段保持一致 */
    int VECTOR_DIMENSION = 1536;

    /**
     * 将文本转换为向量
     *
     * @param text 输入文本（建议长度 ≤ 2000 字符，超出将被截断）
     * @return 1536 维浮点数向量
     */
    float[] embed(String text);

    /**
     * 服务是否可用（API Key 配置正确且网络可达）
     *
     * @return true 表示可用
     */
    boolean isAvailable();
}

package com.blog.article.infrastructure.vector.impl;

import com.blog.article.domain.entity.ArticleEntity;
import com.blog.article.infrastructure.mapper.ArticleMapper;
import com.blog.article.infrastructure.vector.EmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Random;

/**
 * Mock Embedding 服务实现
 *
 * <p>
 * 用于演示和测试的Mock实现，生成随机向量。
 * </p>
 *
 * <p>
 * 特性：
 * </p>
 * <ul>
 * <li>零配置：不依赖外部API</li>
 * <li>立即可用：适合快速开发和演示</li>
 * <li>随机向量：保证每篇文章的向量都不同</li>
 * <li>可替换：后期可无缝切换到真实API</li>
 * </ul>
 *
 * <p>
 * 生产环境替换方案：
 * </p>
 * <ul>
 * <li>创建 OpenAIEmbeddingServiceImpl 实现类</li>
 * <li>配置 embedding.provider=openai</li>
 * <li>无需修改任何调用代码</li>
 * </ul>
 *
 * @author liusxml
 * @since 1.1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "embedding.provider", havingValue = "mock", matchIfMissing = true)
public class MockEmbeddingServiceImpl implements EmbeddingService {

    private final ArticleMapper articleMapper;
    private final Random random = new Random();

    @Override
    public float[] generateEmbedding(String text) {
        log.debug("生成Mock向量: text前100字符={}",
                text.length() > 100 ? text.substring(0, 100) + "..." : text);

        // 生成1536维随机向量
        float[] vector = new float[VECTOR_DIMENSION];

        // 使用文本hashCode作为随机种子，确保相同文本生成相同向量
        Random seededRandom = new Random(text.hashCode());

        for (int i = 0; i < VECTOR_DIMENSION; i++) {
            // 生成-1到1之间的随机浮点数
            vector[i] = (seededRandom.nextFloat() * 2) - 1;
        }

        // 归一化向量（使其模长为1，便于余弦相似度计算）
        normalizeVector(vector);

        log.debug("Mock向量生成成功: 维度={}", vector.length);

        return vector;
    }

    @Async
    @Override
    public void generateAndSaveAsync(Long articleId) {
        log.info("异步生成文章向量: articleId={}", articleId);

        try {
            // 1. 加载文章
            ArticleEntity article = articleMapper.selectById(articleId);
            if (article == null) {
                log.warn("文章不存在，跳过向量生成: articleId={}", articleId);
                return;
            }

            // 2. 拼接文本（标题 + 摘要 + 部分内容）
            String text = buildEmbeddingText(article);

            // 3. 生成向量
            float[] vector = generateEmbedding(text);

            // 4. 转换为MySQL VECTOR格式
            String vectorString = vectorToString(vector);

            // 5. 更新到数据库
            article.setEmbedding(vectorString);
            articleMapper.updateById(article);

            log.info("文章向量生成并保存成功: articleId={}, 向量长度={}",
                    articleId, vectorString.length());

        } catch (Exception e) {
            log.error("文章向量生成失败: articleId={}", articleId, e);
            // 不抛异常，避免影响主业务
        }
    }

    @Override
    public boolean isAvailable() {
        // Mock服务始终可用
        return true;
    }

    /**
     * 构建用于生成向量的文本
     *
     * <p>
     * 策略：标题权重大于内容
     * </p>
     */
    private String buildEmbeddingText(ArticleEntity article) {
        StringBuilder sb = new StringBuilder();

        // 标题（重复3次以增加权重）
        if (article.getTitle() != null) {
            sb.append(article.getTitle()).append(" ");
            sb.append(article.getTitle()).append(" ");
            sb.append(article.getTitle()).append(" ");
        }

        // 摘要
        if (article.getSummary() != null) {
            sb.append(article.getSummary()).append(" ");
        }

        // 内容前500字符
        if (article.getContent() != null) {
            String content = article.getContent();
            int maxLength = Math.min(500, content.length());
            sb.append(content, 0, maxLength);
        }

        return sb.toString();
    }

    /**
     * 归一化向量（L2范数归一化）
     *
     * <p>
     * 使向量模长为1，便于余弦相似度计算。
     * </p>
     */
    private void normalizeVector(float[] vector) {
        double sum = 0.0;
        for (float v : vector) {
            sum += v * v;
        }

        double magnitude = Math.sqrt(sum);

        if (magnitude > 0) {
            for (int i = 0; i < vector.length; i++) {
                vector[i] /= magnitude;
            }
        }
    }
}

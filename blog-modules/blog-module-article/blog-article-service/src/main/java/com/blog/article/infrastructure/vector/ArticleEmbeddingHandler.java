package com.blog.article.infrastructure.vector;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.ai.api.service.TextEmbeddingService;
import com.blog.article.domain.entity.ArticleEntity;
import com.blog.article.infrastructure.mapper.ArticleMapper;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 文章向量生成处理器
 *
 * <p>
 * 负责在文章发布、更新或删除时，管理 Qdrant Cloud 和 MySQL 中的向量数据。
 * </p>
 *
 * <p>
 * 流程：
 * </p>
 * <ol>
 *   <li>从数据库加载文章实体</li>
 *   <li>构建 Embedding 文本（标题重复 × 3 + 摘要 + 内容前 500 字）</li>
 *   <li>调用 {@link TextEmbeddingService#embed} 生成 1024 维向量</li>
 *   <li>删除 Qdrant 中的旧向量（幂等），写入新向量（Qdrant + MySQL 双写）</li>
 * </ol>
 *
 * <p>
 * 调用方：
 * </p>
 * <ul>
 *   <li>{@code ArticleServiceImpl.publishArticle()} — 发布后异步调用 {@link #generateAndSaveAsync(Long)}</li>
 *   <li>{@code ArticleServiceImpl.updateByDto()} — 编辑保存后异步刷新</li>
 *   <li>{@code ArticleServiceImpl.removeById()} — 删除后异步调用 {@link #removeAsync(Long)}</li>
 * </ul>
 *
 * @author liusxml
 * @since 1.2.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ArticleEmbeddingHandler {

    private final TextEmbeddingService embeddingService;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final ArticleMapper articleMapper;

    /** 内容截取最大字符数（防止超出 Token 上限） */
    private static final int MAX_CONTENT_CHARS = 500;

    // ─── 写入 ───────────────────────────────────────────────────────────────

    /**
     * 异步生成并存储文章向量（幂等：先删旧再写新）
     *
     * <p>
     * 由 {@code @Async} 驱动，异步执行，不阻塞文章发布/编辑主流程。
     * 任何异常内部处理，不向调用方传播。
     * </p>
     *
     * @param articleId 文章 ID
     */
    @Async
    public void generateAndSaveAsync(Long articleId) {
        log.info("Async embedding generation started: articleId={}", articleId);
        generateAndSave(articleId);
    }

    /**
     * 同步生成并存储文章向量（供批量重建使用）
     *
     * @param articleId 文章 ID
     */
    public void generateAndSaveSync(Long articleId) {
        generateAndSave(articleId);
    }

    // ─── 删除 ───────────────────────────────────────────────────────────────

    /**
     * 异步删除 Qdrant 中的文章向量
     *
     * <p>
     * 文章被逻辑删除后调用，清理 Qdrant 中的幽灵数据。
     * MySQL embedding 列会随逻辑删除一起不可见，无需额外处理。
     * </p>
     *
     * @param articleId 文章 ID
     */
    @Async
    public void removeAsync(Long articleId) {
        try {
            embeddingStore.removeAll(
                    MetadataFilterBuilder.metadataKey("articleId")
                            .isEqualTo(articleId.toString()));
            log.info("Embedding removed from Qdrant: articleId={}", articleId);
        } catch (Exception e) {
            // 不影响主业务，仅记录日志
            log.error("Failed to remove embedding from Qdrant: articleId={}", articleId, e);
        }
    }

    // ─── 批量重建 ─────────────────────────────────────────────────────────────

    /**
     * 批量重建所有已发布文章的 embedding（管理端使用）
     *
     * <p>
     * 同步逐篇处理，适合一次性迁移场景（如 MySQL → Qdrant 迁移）。
     * 每篇文章先删旧再写新，保证幂等。
     * </p>
     *
     * @return 成功重建的文章数
     */
    public int rebuildAll() {
        List<ArticleEntity> articles = articleMapper.selectList(
                new LambdaQueryWrapper<ArticleEntity>()
                        .eq(ArticleEntity::getStatus, 2)      // 已发布
                        .eq(ArticleEntity::getIsDeleted, 0));  // 未删除
        log.info("Rebuild all embeddings: found {} published articles", articles.size());

        int successCount = 0;
        for (ArticleEntity article : articles) {
            try {
                generateAndSave(article.getId());
                successCount++;
                log.debug("Rebuild embedding succeeded: articleId={} ({}/{})",
                        article.getId(), successCount, articles.size());
            } catch (Exception e) {
                log.error("Rebuild embedding failed: articleId={}", article.getId(), e);
            }
        }

        log.info("Rebuild all embeddings completed: {}/{} succeeded", successCount, articles.size());
        return successCount;
    }

    // ─── 内部方法 ─────────────────────────────────────────────────────────────

    /**
     * 核心逻辑：生成并存储文章向量（幂等）
     *
     * <p>
     * 步骤：加载文章 → 构建文本 → 向量化 → 删旧 → 写新（Qdrant + MySQL 双写）
     * </p>
     */
    private void generateAndSave(Long articleId) {
        try {
            // 1. 加载文章
            ArticleEntity article = articleMapper.selectById(articleId);
            if (article == null) {
                log.warn("Article not found, skip embedding: articleId={}", articleId);
                return;
            }

            // 2. 构建 Embedding 文本
            String text = buildEmbeddingText(article);
            if (StringUtils.isBlank(text)) {
                log.warn("Empty text for article, skip embedding: articleId={}", articleId);
                return;
            }

            // 3. 向量化
            float[] vector = embeddingService.embed(text);
            Embedding embedding = Embedding.from(vector);

            // 4. 构建 TextSegment（Metadata 中写入 articleId 和 title）
            Metadata metadata = Metadata.from("articleId", String.valueOf(articleId))
                    .put("title", article.getTitle() != null ? article.getTitle() : "");
            TextSegment segment = TextSegment.from(text, metadata);

            // 5a. Qdrant：先删旧向量（幂等，不存在也不报错），再写新向量
            embeddingStore.removeAll(
                    MetadataFilterBuilder.metadataKey("articleId")
                            .isEqualTo(articleId.toString()));
            embeddingStore.add(embedding, segment);

            // 5b. MySQL：备份写入 embedding 列（双写）
            articleMapper.updateEmbedding(articleId, Arrays.toString(vector));

            log.info("Embedding generation completed (dual-write): articleId={}, vectorDim={}",
                    articleId, vector.length);

        } catch (Exception e) {
            // 不影响主业务，仅记录日志
            log.error("Embedding generation failed: articleId={}", articleId, e);
        }
    }

    /**
     * 构建用于向量化的文章文本
     *
     * <p>
     * 策略：标题权重高（重复 3 次） + 摘要 + 内容前 {@value MAX_CONTENT_CHARS} 字符
     * </p>
     */
    private String buildEmbeddingText(ArticleEntity article) {
        StringBuilder sb = new StringBuilder();

        // 标题（权重 × 3）
        if (StringUtils.isNotBlank(article.getTitle())) {
            String title = article.getTitle() + " ";
            sb.append(title).append(title).append(title);
        }

        // 摘要
        if (StringUtils.isNotBlank(article.getSummary())) {
            sb.append(article.getSummary()).append(" ");
        }

        // 内容截取
        if (StringUtils.isNotBlank(article.getContent())) {
            sb.append(StringUtils.left(article.getContent(), MAX_CONTENT_CHARS));
        }

        return sb.toString().trim();
    }
}

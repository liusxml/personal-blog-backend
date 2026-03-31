package com.blog.ai.service.impl;

import com.blog.ai.api.dto.AskRequestDTO;
import com.blog.ai.api.dto.AskResponseDTO;
import com.blog.ai.api.dto.ArticleSummaryDTO;
import com.blog.ai.api.service.RagService;
import com.blog.ai.api.service.TextEmbeddingService;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * RAG 问答服务实现
 *
 * <p>
 * 流程：问题向量化 → 召回相关文章（Top-5）→ 拼接 Prompt → LLM 生成回答
 * </p>
 *
 * <p>
 * 依赖说明：
 * </p>
 * <ul>
 *   <li>{@link TextEmbeddingService}：将问题文本转为向量</li>
 *   <li>{@link ChatLanguageModel}：LangChain4j 自动注入 DashScope Chat 模型</li>
 *   <li>{@link EmbeddingStore}：由 {@code QdrantEmbeddingStoreConfig} 配置的 Qdrant Cloud 向量存储提供</li>
 * </ul>
 *
 * @author liusxml
 * @since 1.2.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagServiceImpl implements RagService {

    private final TextEmbeddingService embeddingService;

    /** LangChain4j 通过 dashscope-spring-boot-starter 自动注入 QwenChatModel */
    private final ChatModel chatModel;

    /**
     * 由 QdrantEmbeddingStoreConfig 注册的 QdrantEmbeddingStore 实现。
     */
    private final EmbeddingStore<TextSegment> embeddingStore;

    private static final int MAX_RESULTS = 5;
    private static final double MIN_SCORE = 0.5;

    @Override
    public AskResponseDTO ask(AskRequestDTO request) {
        log.info("RAG ask: question={}", request.getQuestion());

        // 1. 问题 → 向量
        float[] queryVector = embeddingService.embed(request.getQuestion());
        Embedding queryEmbedding = Embedding.from(queryVector);

        // 2. 向量相似度搜索（LangChain4j 标准 API）
        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(MAX_RESULTS)
                .minScore(MIN_SCORE)
                .build();
        List<EmbeddingMatch<TextSegment>> matches = embeddingStore.search(searchRequest).matches();

        log.info("Retrieved {} relevant article(s)", matches.size());

        if (matches.isEmpty()) {
            return new AskResponseDTO("抱歉，暂时没有找到与您问题相关的博客文章。", List.of());
        }

        // 3. 构建 Prompt（将召回的文章内容注入上下文）
        String context = matches.stream()
                .map(m -> m.embedded().text())
                .collect(Collectors.joining("\n\n---\n\n"));

        String prompt = """
                你是一个博客助手，请严格基于以下博客文章内容回答用户的问题。
                如果文章中没有相关信息，请如实告知，不要编造答案。
                
                【博客文章内容】
                %s
                
                【用户问题】
                %s
                """.formatted(context, request.getQuestion());

        // 4. LLM 生成回答
        String answer = chatModel.chat(prompt);
        log.info("LLM answer generated, length={}", answer.length());

        // 5. 转换来源文章摘要
        List<ArticleSummaryDTO> sources = toSources(matches);

        return new AskResponseDTO(answer, sources);
    }

    /**
     * 将 EmbeddingMatch 列表转换为文章摘要 DTO 列表
     *
     * <p>
     * Qdrant 在 add() 时会将 articleId、title 存入 TextSegment 的 Metadata，
     * 此处从 Metadata 中读取。
     * </p>
     */
    private List<ArticleSummaryDTO> toSources(List<EmbeddingMatch<TextSegment>> matches) {
        return matches.stream()
                .map(match -> {
                    var metadata = match.embedded().metadata();
                    String articleIdStr = metadata.getString("articleId");
                    String title = metadata.getString("title");
                    Long articleId = articleIdStr != null ? Long.parseLong(articleIdStr) : null;

                    return new ArticleSummaryDTO(
                            articleId,
                            title,
                            "/articles/" + articleId,
                            match.score()
                    );
                })
                .collect(Collectors.toList());
    }
}

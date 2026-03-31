package com.blog.ai.service.impl;

import com.blog.ai.api.dto.AskRequestDTO;
import com.blog.ai.api.dto.AskResponseDTO;
import com.blog.ai.api.service.TextEmbeddingService;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * RagServiceImpl 单元测试
 *
 * <p>
 * 使用 Mockito mock 所有外部依赖（TextEmbeddingService / ChatModel / EmbeddingStore），
 * 只验证 RAG 流程中的业务逻辑。
 * </p>
 *
 * @author liusxml
 * @since 1.2.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RagServiceImpl 单元测试")
class RagServiceImplTest {

    @Mock
    private TextEmbeddingService embeddingService;

    @Mock
    private ChatModel chatModel;

    @Mock
    @SuppressWarnings("unchecked")
    private EmbeddingStore<TextSegment> embeddingStore;

    @InjectMocks
    private RagServiceImpl ragService;

    private AskRequestDTO request;
    private float[] dummyVector;

    @BeforeEach
    void setUp() {
        request = new AskRequestDTO();
        request.setQuestion("博客里有哪些关于 Java 的文章？");
        dummyVector = new float[]{0.1f, 0.2f, 0.3f};
    }

    // ─── 正常 RAG 流程 ────────────────────────────────────────────────────────

    @Test
    @DisplayName("正常问答 - 搜索到相关文章时 RAG 全流程执行")
    void should_returnRagAnswer_when_relatedArticlesFound() {
        // Given
        TextSegment segment = buildSegment("1", "Java虚拟线程详解", "Java 21 引入虚拟线程...");
        EmbeddingMatch<TextSegment> match = new EmbeddingMatch<>(0.9, "1", null, segment);
        EmbeddingSearchResult<TextSegment> searchResult = new EmbeddingSearchResult<>(List.of(match));

        when(embeddingService.embed(request.getQuestion())).thenReturn(dummyVector);
        when(embeddingStore.search(any(EmbeddingSearchRequest.class))).thenReturn(searchResult);
        when(chatModel.chat(anyString())).thenReturn("Java 21 虚拟线程是轻量级线程...");

        // When
        AskResponseDTO response = ragService.ask(request);

        // Then
        assertThat(response.getAnswer()).isEqualTo("Java 21 虚拟线程是轻量级线程...");
        assertThat(response.getSources()).hasSize(1);
        assertThat(response.getSources().get(0).getId()).isEqualTo(1L);
        assertThat(response.getSources().get(0).getTitle()).isEqualTo("Java虚拟线程详解");
        assertThat(response.getSources().get(0).getUrl()).isEqualTo("/articles/1");
        assertThat(response.getSources().get(0).getScore()).isEqualTo(0.9);
    }

    @Test
    @DisplayName("正常问答 - Prompt 中包含召回文章内容")
    void should_includeContextInPrompt_when_articlesRetrieved() {
        // Given
        TextSegment segment = buildSegment("2", "Spring Boot 实战", "Spring Boot 是...");
        EmbeddingMatch<TextSegment> match = new EmbeddingMatch<>(0.8, "2", null, segment);
        EmbeddingSearchResult<TextSegment> searchResult = new EmbeddingSearchResult<>(List.of(match));

        when(embeddingService.embed(anyString())).thenReturn(dummyVector);
        when(embeddingStore.search(any())).thenReturn(searchResult);
        when(chatModel.chat(anyString())).thenReturn("回答内容");

        // When
        ragService.ask(request);

        // Then：验证 LLM 收到的 Prompt 含有文章内容和用户问题
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(chatModel).chat(promptCaptor.capture());
        String prompt = promptCaptor.getValue();

        assertThat(prompt).contains("Spring Boot 是...");          // 文章内容注入
        assertThat(prompt).contains("博客里有哪些关于 Java 的文章？"); // 用户问题注入
        assertThat(prompt).contains("博客助手");                   // 系统指令存在
    }

    @Test
    @DisplayName("正常问答 - 多篇文章用分隔符拼接")
    void should_joinMultipleArticlesWithSeparator_when_multipleMatches() {
        // Given
        TextSegment seg1 = buildSegment("1", "文章一", "内容一");
        TextSegment seg2 = buildSegment("2", "文章二", "内容二");
        List<EmbeddingMatch<TextSegment>> matches = List.of(
                new EmbeddingMatch<>(0.9, "1", null, seg1),
                new EmbeddingMatch<>(0.8, "2", null, seg2)
        );
        when(embeddingService.embed(anyString())).thenReturn(dummyVector);
        when(embeddingStore.search(any())).thenReturn(new EmbeddingSearchResult<>(matches));
        when(chatModel.chat(anyString())).thenReturn("综合回答");

        // When
        AskResponseDTO response = ragService.ask(request);

        // Then
        assertThat(response.getSources()).hasSize(2);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(chatModel).chat(captor.capture());
        assertThat(captor.getValue()).contains("---");   // 分隔符存在
        assertThat(captor.getValue()).contains("内容一");
        assertThat(captor.getValue()).contains("内容二");
    }

    // ─── 无搜索结果的兜底逻辑 ───────────────────────────────────────────────────

    @Test
    @DisplayName("无相关文章 - 返回兜底回答，不调用 LLM")
    void should_returnFallbackAnswer_when_noArticlesFound() {
        // Given
        when(embeddingService.embed(anyString())).thenReturn(dummyVector);
        when(embeddingStore.search(any())).thenReturn(new EmbeddingSearchResult<>(List.of()));

        // When
        AskResponseDTO response = ragService.ask(request);

        // Then
        assertThat(response.getAnswer()).contains("没有找到");
        assertThat(response.getSources()).isEmpty();

        // 无搜索结果时不应调用 LLM（节省 Token）
        org.mockito.Mockito.verifyNoInteractions(chatModel);
    }

    // ─── SearchRequest 参数验证 ──────────────────────────────────────────────────

    @Test
    @DisplayName("搜索请求 - maxResults=5 且 minScore=0.5")
    void should_buildCorrectSearchRequest() {
        // Given
        when(embeddingService.embed(anyString())).thenReturn(dummyVector);
        when(embeddingStore.search(any())).thenReturn(new EmbeddingSearchResult<>(List.of()));

        // When
        ragService.ask(request);

        // Then
        ArgumentCaptor<EmbeddingSearchRequest> captor =
                ArgumentCaptor.forClass(EmbeddingSearchRequest.class);
        verify(embeddingStore).search(captor.capture());

        EmbeddingSearchRequest captured = captor.getValue();
        assertThat(captured.maxResults()).isEqualTo(5);
        assertThat(captured.minScore()).isEqualTo(0.5);
    }

    // ─── 私有工具方法 ─────────────────────────────────────────────────────────────

    private TextSegment buildSegment(String articleId, String title, String content) {
        Metadata metadata = Metadata.from("articleId", articleId).put("title", title);
        return TextSegment.from(content, metadata);
    }
}

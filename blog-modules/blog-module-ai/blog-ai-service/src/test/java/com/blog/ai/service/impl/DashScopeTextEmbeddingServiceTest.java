package com.blog.ai.service.impl;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * DashScopeTextEmbeddingService 单元测试
 *
 * <p>
 * 核心验证：文本截断逻辑（≤ 2000 字符）和 EmbeddingModel 的调用。
 * </p>
 *
 * @author liusxml
 * @since 1.2.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DashScopeTextEmbeddingService 单元测试")
class DashScopeTextEmbeddingServiceTest {

    @Mock
    private EmbeddingModel embeddingModel;

    @InjectMocks
    private DashScopeTextEmbeddingService embeddingService;

    // ─── 正常文本 ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("短文本 - 原文不截断，直接传给 EmbeddingModel")
    void should_notTruncate_when_textLengthWithin2000() {
        // Given
        String shortText = "Java 虚拟线程是 Java 21 的重要特性";
        float[] expectedVector = new float[]{0.1f, 0.2f, 0.3f};
        Embedding mockEmbedding = Embedding.from(expectedVector);
        when(embeddingModel.embed(shortText)).thenReturn(Response.from(mockEmbedding));

        // When
        float[] result = embeddingService.embed(shortText);

        // Then
        assertThat(result).isEqualTo(expectedVector);
        verify(embeddingModel).embed(eq(shortText));  // 原文传入，未截断
    }

    @Test
    @DisplayName("长文本 - 超过 2000 字符时截断到 2000 字符")
    void should_truncateTo2000_when_textExceeds2000Chars() {
        // Given：2500 字符的长文本
        String longText = "A".repeat(2500);
        float[] expectedVector = new float[]{0.5f, 0.6f};
        Embedding mockEmbedding = Embedding.from(expectedVector);
        when(embeddingModel.embed(anyString())).thenReturn(Response.from(mockEmbedding));

        // When
        embeddingService.embed(longText);

        // Then：验证传入 EmbeddingModel 的字符串长度为 2000
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(embeddingModel).embed(captor.capture());
        assertThat(captor.getValue()).hasSize(2000);
        assertThat(captor.getValue()).isEqualTo("A".repeat(2000));
    }

    @Test
    @DisplayName("边界值 - 恰好 2000 字符时不截断")
    void should_notTruncate_when_textExactly2000Chars() {
        // Given
        String exactly2000 = "B".repeat(2000);
        Embedding mockEmbedding = Embedding.from(new float[]{0.1f});
        when(embeddingModel.embed(exactly2000)).thenReturn(Response.from(mockEmbedding));

        // When
        embeddingService.embed(exactly2000);

        // Then：原文传入，未被截断
        verify(embeddingModel).embed(eq(exactly2000));
    }

    @Test
    @DisplayName("返回向量 - embed 返回 EmbeddingModel 的 vector()")
    void should_returnVectorFromModel() {
        // Given
        String text = "测试文本";
        float[] modelVector = new float[1536];
        for (int i = 0; i < 1536; i++) {
            modelVector[i] = i * 0.001f;
        }
        Embedding mockEmbedding = Embedding.from(modelVector);
        when(embeddingModel.embed(text)).thenReturn(Response.from(mockEmbedding));

        // When
        float[] result = embeddingService.embed(text);

        // Then
        assertThat(result).hasSize(1536);
        assertThat(result[0]).isEqualTo(0.0f);
        assertThat(result[1]).isEqualTo(0.001f);
    }

    // ─── isAvailable ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("isAvailable - EmbeddingModel 已注入时返回 true")
    void should_returnTrue_when_embeddingModelInjected() {
        assertThat(embeddingService.isAvailable()).isTrue();
    }
}

package com.blog.comment.domain.parser;

import com.blog.system.api.service.IUserQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

/**
 * MentionParser 功能测试
 *
 * @author liusxml
 * @since 1.6.0
 */
@ExtendWith(MockitoExtension.class)
class MentionParserTest {

    @Mock
    private IUserQueryService userQueryService;

    private MentionParser mentionParser;

    @BeforeEach
    void setUp() {
        mentionParser = new MentionParser(userQueryService);
    }

    @Test
    @DisplayName("解析评论中的@mention - 单个用户")
    void should_parseSingleMention_when_commentContainsOneMention() {
        // Given
        String content = "Hello @alice, nice to meet you!";
        when(userQueryService.getUserIdsByUsernames(anyCollection()))
                .thenReturn(List.of(100L));

        // When
        Set<Long> result = mentionParser.parseMentions(content);

        // Then
        assertThat(result).contains(100L);
        verify(userQueryService, times(1)).getUserIdsByUsernames(anyCollection());
    }

    @Test
    @DisplayName("解析评论中的@mention - 多个用户")
    void should_parseMultipleMentions_when_commentContainsManyMentions() {
        // Given
        String content = "@alice and @bob_123 are here. @charlie too!";
        when(userQueryService.getUserIdsByUsernames(anyCollection()))
                .thenReturn(List.of(100L, 200L, 300L));

        // When
        Set<Long> result = mentionParser.parseMentions(content);

        // Then
        assertThat(result).hasSize(3);
        assertThat(result).containsExactlyInAnyOrder(100L, 200L, 300L);
    }

    @Test
    @DisplayName("去重 - 同一用户被多次@")
    void should_deduplicateMentions_when_sameUserMentionedMultipleTimes() {
        // Given
        String content = "Hi @alice, @alice are you there?";
        when(userQueryService.getUserIdsByUsernames(anyCollection()))
                .thenReturn(List.of(100L));

        // When
        Set<Long> result = mentionParser.parseMentions(content);

        // Then
        assertThat(result).hasSize(1).containsExactly(100L);
    }

    @Test
    @DisplayName("无@mention时返回空集合")
    void should_returnEmpty_when_noMentions() {
        // Given
        String content = "Hello world, no mentions here!";

        // When
        Set<Long> result = mentionParser.parseMentions(content);

        // Then
        assertThat(result).isEmpty();
        verify(userQueryService, never()).getUserIdsByUsernames(anyCollection());
    }

    @Test
    @DisplayName("空内容返回空集合")
    void should_returnEmpty_when_contentIsBlank() {
        // When
        Set<Long> result1 = mentionParser.parseMentions("");
        Set<Long> result2 = mentionParser.parseMentions(null);

        // Then
        assertThat(result1).isEmpty();
        assertThat(result2).isEmpty();
    }

    @Test
    @DisplayName("高亮@mention")
    void should_highlightMentions_when_htmlContainsMention() {
        // Given
        String html = "<p>Hello @alice and @bob</p>";

        // When
        String result = mentionParser.highlightMentions(html);

        // Then
        assertThat(result).contains("<span class=\"mention\">@alice</span>");
        assertThat(result).contains("<span class=\"mention\">@bob</span>");
    }
}

package com.blog.file.service.impl;

import com.blog.article.api.dto.ArticleDTO;
import com.blog.article.api.enums.ArticleStatus;
import com.blog.article.api.vo.ArticleDetailVO;
import com.blog.article.api.vo.ArticleListVO;
import com.blog.article.domain.entity.ArticleEntity;
import com.blog.article.domain.event.ArticlePublishedEvent;
import com.blog.article.domain.state.ArticleState;
import com.blog.article.domain.state.ArticleStateFactory;
import com.blog.article.infrastructure.converter.ArticleConverter;
import com.blog.article.infrastructure.mapper.ArticleMapper;
import com.blog.article.metrics.ArticleMetrics;
import com.blog.article.service.BingWallpaperService;
import com.blog.article.service.chain.ContentProcessor;
import com.blog.article.service.chain.ProcessResult;
import com.blog.article.service.impl.ArticleServiceImpl;
import com.blog.ai.api.service.TextEmbeddingService;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ArticleServiceImpl 单元测试
 *
 * <p>
 * 测试覆盖：
 * </p>
 * <ul>
 * <li>CRUD基础操作</li>
 * <li>状态流转（发布、归档、恢复）</li>
 * <li>内容处理链集成</li>
 * <li>向量搜索集成</li>
 * <li>异常处理</li>
 * </ul>
 *
 * @author liusxml
 * @since 1.1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ArticleServiceImpl 单元测试")
class ArticleServiceImplTest {

    @Mock
    private ArticleMapper articleMapper;

    @Mock
    private ArticleConverter converter;

    @Mock
    private ArticleStateFactory stateFactory;

    @Mock
    private ContentProcessor contentProcessorChain;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private BingWallpaperService bingWallpaperService;

    @Mock
    private ArticleMetrics articleMetrics;

    /** Qdrant 语义搜索所需的两个新 Bean（Phase 7 新增） */
    @Mock
    private TextEmbeddingService embeddingService;

    @Mock
    private EmbeddingStore<TextSegment> embeddingStore;

    @InjectMocks
    private ArticleServiceImpl articleService;

    private ArticleDTO testDTO;
    private ArticleEntity testEntity;
    private ArticleDetailVO testVO;

    @BeforeEach
    void setUp() {
        // MyBatis-Plus ServiceImpl.baseMapper 需要通过反射注入 mock
        // 否则 archiveArticle/publishArticle 等方法内用 baseMapper 时会 NPE
        ReflectionTestUtils.setField(articleService, "baseMapper", articleMapper);

        // 准备测试数据
        testDTO = new ArticleDTO();
        testDTO.setTitle("测试文章");
        testDTO.setContent("# 测试内容");
        testDTO.setSummary("测试摘要");
        testDTO.setCategoryId(1L);

        testEntity = new ArticleEntity();
        testEntity.setId(1L);
        testEntity.setTitle("测试文章");
        testEntity.setContent("# 测试内容");
        testEntity.setStatus(ArticleStatus.DRAFT.getCode());

        testVO = new ArticleDetailVO();
        testVO.setId(String.valueOf(1L));
        testVO.setTitle("测试文章");
    }

    // ==================== 发布相关测试 ====================

    @Test
    @DisplayName("发布文章 - 草稿状态成功发布")
    void should_publishSuccess_when_articleIsDraft() {
        // Given
        Long articleId = 1L;
        testEntity.setStatus(ArticleStatus.DRAFT.getCode());

        ArticleState mockState = mock(ArticleState.class);

        when(articleMapper.selectById(articleId)).thenReturn(testEntity);
        when(stateFactory.getState(testEntity)).thenReturn(mockState);

        // When
        articleService.publishArticle(articleId);

        // Then
        verify(mockState).publish(testEntity);
        verify(articleMapper).updateById(testEntity);

        // 验证事件发布
        ArgumentCaptor<ArticlePublishedEvent> eventCaptor = ArgumentCaptor.forClass(ArticlePublishedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        ArticlePublishedEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getArticleId()).isEqualTo(Long.valueOf(articleId));
    }

    @Test
    @DisplayName("发布文章 - 文章不存在抛出异常")
    void should_throwException_when_articleNotExist() {
        // Given
        Long articleId = 999L;
        when(articleMapper.selectById(articleId)).thenReturn(null);

        // When & Then
        // publishArticle() 内部用 baseMapper，找不到文章时抛 IllegalArgumentException
        assertThatThrownBy(() -> articleService.publishArticle(articleId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("文章不存在");
    }

    // ==================== 归档相关测试 ====================

    @Test
    @DisplayName("归档文章 - 已发布状态成功归档")
    void should_archiveSuccess_when_articleIsPublished() {
        // Given
        Long articleId = 1L;
        testEntity.setStatus(ArticleStatus.PUBLISHED.getCode());

        ArticleState mockState = mock(ArticleState.class);

        when(articleMapper.selectById(articleId)).thenReturn(testEntity);
        when(stateFactory.getState(testEntity)).thenReturn(mockState);

        // When
        articleService.archiveArticle(articleId);

        // Then
        verify(mockState).archive(testEntity);
        verify(articleMapper).updateById(testEntity);
    }

    @Test
    @DisplayName("归档文章 - 文章不存在抛出异常")
    void should_throwException_when_archiveNonExistArticle() {
        // Given
        Long articleId = 999L;
        when(articleMapper.selectById(articleId)).thenReturn(null);

        // When & Then
        // archiveArticle() 内部用 baseMapper，招不到返回 null 时抛 IllegalArgumentException
        assertThatThrownBy(() -> articleService.archiveArticle(articleId))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ==================== 恢复归档测试 ====================

    @Test
    @DisplayName("恢复归档 - 已归档状态成功恢复")
    void should_unarchiveSuccess_when_articleIsArchived() {
        // Given
        Long articleId = 1L;
        testEntity.setStatus(ArticleStatus.ARCHIVED.getCode());

        ArticleState mockState = mock(ArticleState.class);

        when(articleMapper.selectById(articleId)).thenReturn(testEntity);
        when(stateFactory.getState(testEntity)).thenReturn(mockState);

        // When
        articleService.unarchiveArticle(articleId);

        // Then
        verify(mockState).unarchive(testEntity);
        verify(articleMapper).updateById(testEntity);
    }

    // ==================== 相关文章推荐测试 ====================

    @Test
    @DisplayName("获取相关文章 - Qdrant 搜索失败降级到同分类")
    void should_fallbackToCategory_when_qdrantSearchFails() throws Exception {
        // Given
        Long articleId = 1L;
        Integer limit = 5;

        ArticleEntity article = new ArticleEntity();
        article.setId(articleId);
        article.setTitle("测试文章");
        article.setCategoryId(1L);

        ArticleEntity catEntity = new ArticleEntity();
        catEntity.setId(3L);
        catEntity.setTitle("同分类文章");

        ArticleListVO catVO = new ArticleListVO();
        catVO.setId(String.valueOf(3L));
        catVO.setTitle("同分类文章");

        when(articleMapper.selectById(articleId)).thenReturn(article);
        // Qdrant embed 抛出异常，触发降级逻辑
        when(embeddingService.embed(any(String.class)))
                .thenThrow(new RuntimeException("Qdrant connection failed"));
        when(articleMapper.findByCategoryExcluding(1L, articleId, limit))
                .thenReturn(List.of(catEntity));
        when(converter.entityToListVo(catEntity)).thenReturn(catVO);

        // When
        List<ArticleListVO> result = articleService.getRelatedArticles(articleId, limit);

        // Then — Qdrant 失败后降级到同分类
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("同分类文章");
        verify(articleMapper).findByCategoryExcluding(1L, articleId, limit);
    }

    @Test
    @DisplayName("获取相关文章 - 标题为空时直接降级到同分类")
    void should_fallbackToCategory_when_articleHasNoTitle() {
        // Given
        Long articleId = 1L;
        Integer limit = 5;

        ArticleEntity articleNoTitle = new ArticleEntity();
        articleNoTitle.setId(articleId);
        articleNoTitle.setTitle(null); // 无标题，跳过 Qdrant 搜索直接降级
        articleNoTitle.setCategoryId(1L);

        ArticleEntity catEntity = new ArticleEntity();
        catEntity.setId(3L);
        catEntity.setTitle("同分类文章");

        ArticleListVO catVO = new ArticleListVO();
        catVO.setId(String.valueOf(3L));

        when(articleMapper.selectById(articleId)).thenReturn(articleNoTitle);
        when(articleMapper.findByCategoryExcluding(1L, articleId, limit))
                .thenReturn(List.of(catEntity));
        when(converter.entityToListVo(catEntity)).thenReturn(catVO);

        // When
        List<ArticleListVO> result = articleService.getRelatedArticles(articleId, limit);

        // Then
        assertThat(result).hasSize(1);
        verify(articleMapper).findByCategoryExcluding(1L, articleId, limit);
    }

    @Test
    @DisplayName("获取相关文章 - 文章不存在返回空列表")
    void should_returnEmptyList_when_articleNotFound() {
        // Given
        Long articleId = 999L;
        Integer limit = 5;

        when(articleMapper.selectById(articleId)).thenReturn(null);

        // When
        List<ArticleListVO> result = articleService.getRelatedArticles(articleId, limit);

        // Then
        assertThat(result).isEmpty();
    }

    // ==================== 内容处理链测试 ====================

    @Test
    @DisplayName("preSave钩子 - 内容处理链被正确调用")
    void should_callContentProcessorChain_when_preSave() {
        // Given
        ArticleEntity entity = new ArticleEntity();
        entity.setTitle("测试");
        entity.setContent("# Markdown内容");
        entity.setCoverImage("/img/cover.jpg");  // 设置封面，跳过 bingWallpaperService 调用
        entity.setAuthorId(1L);                   // 设置作者ID，跳过 SecurityUtils 调用

        ProcessResult mockResult = new ProcessResult();
        mockResult.setSuccess(true);
        mockResult.setHtml("<h1>Markdown内容</h1>");
        mockResult.setTocJson("[{\"level\":1,\"title\":\"Markdown内容\"}]");
        mockResult.setSummary("自动提取的摘要");

        when(contentProcessorChain.process(any(ProcessResult.class)))
                .thenReturn(mockResult);

        // When
        // 通过反射调用 preSave (因为是 protected 方法)
        try {
            var method = ArticleServiceImpl.class.getDeclaredMethod("preSave", ArticleEntity.class);
            method.setAccessible(true);
            method.invoke(articleService, entity);
        } catch (Exception e) {
            fail("反射调用失败", e);
        }

        // Then
        verify(contentProcessorChain).process(any(ProcessResult.class));
        assertThat(entity.getContentHtml()).isEqualTo("<h1>Markdown内容</h1>");
        assertThat(entity.getTocJson()).isNotNull();
        assertThat(entity.getSummary()).isEqualTo("自动提取的摘要");
    }

    @Test
    @DisplayName("preSave钩子 - 默认值正确设置")
    void should_setDefaultValues_when_preSave() {
        // Given
        ArticleEntity entity = new ArticleEntity();
        entity.setTitle("测试");
        entity.setContent("内容");
        entity.setCoverImage("/img/cover.jpg");  // 设置封面，跳过 bingWallpaperService 调用
        entity.setAuthorId(1L);                   // 设置作者ID，跳过 SecurityUtils 调用

        ProcessResult mockResult = new ProcessResult();
        mockResult.setSuccess(true);
        mockResult.setHtml("HTML");
        mockResult.setTocJson("[]");
        mockResult.setSummary("摘要");

        when(contentProcessorChain.process(any(ProcessResult.class)))
                .thenReturn(mockResult);

        // When
        try {
            var method = ArticleServiceImpl.class.getDeclaredMethod("preSave", ArticleEntity.class);
            method.setAccessible(true);
            method.invoke(articleService, entity);
        } catch (Exception e) {
            fail("反射调用失败", e);
        }

        // Then
        assertThat(entity.getStatus()).isEqualTo(ArticleStatus.DRAFT.getCode());
        assertThat(entity.getIsTop()).isEqualTo(0);
        assertThat(entity.getIsFeatured()).isEqualTo(0);
        assertThat(entity.getIsCommentDisabled()).isEqualTo(0);
    }

    // ==================== 边界条件测试 ====================

    @Test
    @DisplayName("发布文章 - Null参数抛出异常")
    void should_throwException_when_publishWithNullId() {
        // When & Then
        assertThatThrownBy(() -> articleService.publishArticle(null))
                .isInstanceOf(Exception.class);
    }
}

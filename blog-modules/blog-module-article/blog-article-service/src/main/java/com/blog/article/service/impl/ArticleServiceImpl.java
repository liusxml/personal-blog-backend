package com.blog.article.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.article.domain.entity.ArticleEntity;
import com.blog.article.domain.event.ArticlePublishedEvent;
import com.blog.article.domain.state.ArticleState;
import com.blog.article.domain.state.ArticleStateFactory;
import com.blog.article.infrastructure.converter.ArticleConverter;
import com.blog.article.infrastructure.mapper.ArticleMapper;
import com.blog.article.infrastructure.vector.VectorSearchService;
import com.blog.article.service.IArticleService;
import com.blog.article.service.chain.ContentProcessor;
import com.blog.article.service.chain.ProcessResult;
import com.blog.common.base.BaseServiceImpl;
import com.blog.common.exception.BusinessException;
import com.blog.common.exception.SystemErrorCode;
import com.blog.common.model.PageResult;
import com.blog.common.utils.SecurityUtils;
import com.blog.dto.ArticleDTO;
import com.blog.dto.ArticleQueryDTO;
import com.blog.enums.ArticleStatus;
import com.blog.vo.ArticleDetailVO;
import com.blog.vo.ArticleListVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * 文章服务实现
 *
 * <p>
 * 遵循项目规范：
 * </p>
 * <ul>
 * <li>继承 BaseServiceImpl 获得 CRUD 能力</li>
 * <li>使用构造注入（显式调用 super）</li>
 * <li>抛出 BusinessException 处理异常</li>
 * <li>使用 @Slf4j 记录日志</li>
 * </ul>
 *
 * <p>
 * 核心功能：
 * </p>
 * <ul>
 * <li>文章发布/归档/恢复（状态流转）</li>
 * <li>相关文章推荐（向量搜索）</li>
 * <li>浏览量统计（异步更新）</li>
 * <li>访问权限校验</li>
 * </ul>
 *
 * @author liusxml
 * @since 1.0.0
 */
@Slf4j
@Service
public class ArticleServiceImpl
        extends BaseServiceImpl<ArticleMapper, ArticleEntity, ArticleDetailVO, ArticleDTO, ArticleConverter>
        implements IArticleService {

    private final ArticleConverter converter;
    private final ArticleStateFactory stateFactory;
    private final ContentProcessor contentProcessorChain;
    private final ApplicationEventPublisher eventPublisher;
    private final VectorSearchService vectorSearchService;

    /**
     * 调用父类构造函数注入 converter
     */
    public ArticleServiceImpl(ArticleConverter converter,
            ArticleStateFactory stateFactory,
            ContentProcessor contentProcessorChain,
            ApplicationEventPublisher eventPublisher,
            VectorSearchService vectorSearchService) {
        super(converter);
        this.converter = converter;
        this.stateFactory = stateFactory;
        this.contentProcessorChain = contentProcessorChain;
        this.eventPublisher = eventPublisher;
        this.vectorSearchService = vectorSearchService;
    }


    /**
     * 保存前钩子：设置默认值 + 内容处理
     *
     * <p>
     * 使用责任链模式处理内容：
     * </p>
     * <ol>
     * <li>XSS过滤</li>
     * <li>Markdown转HTML</li>
     * <li>生成目录</li>
     * <li>提取摘要</li>
     * </ol>
     *
     * @param entity 实体
     */
    @Override
    protected void preSave(ArticleEntity entity) {
        log.info("创建文章: title={}", entity.getTitle());

        // 设置默认状态为草稿
        if (entity.getStatus() == null) {
            entity.setStatus(ArticleStatus.DRAFT.getCode());
        }

        // 设置作者ID为当前登录用户（必填字段）
        if (entity.getAuthorId() == null) {
            Long currentUserId = SecurityUtils.getCurrentUserId();
            if (currentUserId != null) {
                entity.setAuthorId(currentUserId);
                log.debug("自动设置作者ID: authorId={}", currentUserId);
            } else {
                log.error("创建文章失败: 无法获取当前登录用户");
                throw new BusinessException(SystemErrorCode.UNAUTHORIZED);
            }
        }

        // 设置默认特性标记（使用 Objects.requireNonNullElse 替代 if-null 检查）
        entity.setIsTop(Objects.requireNonNullElse(entity.getIsTop(), 0));
        entity.setIsFeatured(Objects.requireNonNullElse(entity.getIsFeatured(), 0));
        entity.setIsCommentDisabled(Objects.requireNonNullElse(entity.getIsCommentDisabled(), 0));

        // 使用责任链处理内容
        if (StringUtils.isNotBlank(entity.getContent())) {
            ProcessResult result = new ProcessResult();
            result.setMarkdown(entity.getContent());

            // 通过处理链
            result = contentProcessorChain.process(result);

            if (result.isSuccess()) {
                entity.setContentHtml(result.getHtml());
                entity.setTocJson(result.getTocJson());

                // 如果没有手动填写摘要，使用自动提取的
                if (StringUtils.isBlank(entity.getSummary())) {
                    entity.setSummary(result.getSummary());
                }

                log.debug("内容处理完成: HTML长度={}, TOC长度={}, 摘要长度={}",
                        result.getHtml().length(),
                        result.getTocJson().length(),
                        result.getSummary().length());
            } else {
                log.error("内容处理失败: {}", result.getErrorMessage());
            }
        }
    }

    /**
     * 更新前钩子
     *
     * @param entity 实体
     */
    @Override
    protected void preUpdate(ArticleEntity entity) {
        log.info("更新文章: id={}, title={}", entity.getId(), entity.getTitle());
    }

    /**
     * 发布文章
     *
     * <p>
     * 使用状态模式处理状态流转，避免 if-else。
     * </p>
     *
     * <p>
     * 副作用：
     * </p>
     * <ul>
     * <li>设置发布时间</li>
     * <li>触发 ArticlePublishedEvent (TODO)</li>
     * <li>初始化统计数据（由监听器处理）</li>
     * <li>生成向量（由监听器异步处理）</li>
     * </ul>
     *
     * @param articleId 文章ID
     */
    @Override
    public void publishArticle(Long articleId) {
        log.info("发布文章: articleId={}", articleId);

        ArticleEntity article = baseMapper.selectById(articleId);
        if (article == null) {
            throw new IllegalArgumentException("文章不存在");
        }

        // 使用状态模式处理发布逻辑
        ArticleState state = stateFactory.getState(article);
        state.publish(article);

        // 更新数据库
        baseMapper.updateById(article);

        // 发布 ArticlePublishedEvent（异步处理副作用）
        ArticlePublishedEvent event = new ArticlePublishedEvent(
                this,
                article.getId(),
                article.getAuthorId(),
                article.getTitle());
        eventPublisher.publishEvent(event);

        log.info("文章发布成功: id={}, 事件已发布", articleId);
    }

    /**
     * 归档文章
     *
     * <p>
     * 使用状态模式处理状态流转。
     * </p>
     *
     * @param articleId 文章ID
     */
    @Override
    public void archiveArticle(Long articleId) {
        log.info("归档文章: articleId={}", articleId);

        ArticleEntity article = baseMapper.selectById(articleId);
        if (article == null) {
            throw new IllegalArgumentException("文章不存在");
        }

        // 使用状态模式处理归档逻辑
        ArticleState state = stateFactory.getState(article);
        state.archive(article);

        baseMapper.updateById(article);

        log.info("文章归档成功: id={}", articleId);
    }

    /**
     * 恢复归档文章
     *
     * <p>
     * 使用状态模式处理状态流转。
     * </p>
     *
     * @param articleId 文章ID
     */
    @Override
    public void unarchiveArticle(Long articleId) {
        log.info("恢复归档文章: articleId={}", articleId);

        ArticleEntity article = baseMapper.selectById(articleId);
        if (article == null) {
            throw new IllegalArgumentException("文章不存在");
        }

        // 使用状态模式处理恢复逻辑
        ArticleState state = stateFactory.getState(article);
        state.unarchive(article);

        baseMapper.updateById(article);

        log.info("文章恢复成功: id={}", articleId);
    }

    /**
     * 获取相关文章推荐（基于向量搜索）
     *
     * <p>
     * 使用MySQL 9.4 VECTOR相似度搜索。
     * </p>
     *
     * <p>
     * 降级策略：
     * </p>
     * <ul>
     * <li>如果文章没有向量，返回同分类文章</li>
     * <li>如果查询失败，返回最新文章</li>
     * </ul>
     *
     * @param articleId 文章ID
     * @param limit     返回数量
     * @return 相关文章列表
     */
    @Override
    public List<ArticleListVO> getRelatedArticles(Long articleId, Integer limit) {
        log.debug("获取相关文章: articleId={}, limit={}", articleId, limit);

        // 调用向量搜索服务
        return vectorSearchService.findRelatedArticles(articleId, limit);
    }

    /**
     * 增加浏览量
     *
     * <p>
     * TODO: 异步更新统计表
     * </p>
     *
     * @param articleId 文章ID
     */
    @Override
    public void incrementViewCount(Long articleId) {
        log.debug("增加浏览量: articleId={}", articleId);
        // 待实现: 异步更新 art_article_stats
    }

    /**
     * 检查文章访问权限
     *
     * <p>
     * 验证规则：
     * </p>
     * <ul>
     * <li>草稿状态：仅作者可见</li>
     * <li>已发布/已归档：公开可见</li>
     * <li>加密文章：需要密码</li>
     * </ul>
     *
     * @param articleId 文章ID
     * @param password  访问密码（可选）
     * @return true 如果允许访问
     */
    @Override
    public boolean checkAccessPermission(Long articleId, String password) {
        ArticleEntity article = baseMapper.selectById(articleId);
        if (article == null) {
            return false;
        }

        // 待实现: 作者权限检查

        // 检查密码保护
        if (StringUtils.isNotBlank(article.getPassword())) {
            return article.getPassword().equals(password);
        }

        // 检查发布状态
        return ArticleStatus.of(article.getStatus()).isPublic();
    }

    /**
     * 分页查询文章列表
     *
     * <p>
     * 支持多种筛选条件：分类、标签、关键词、状态。
     * </p>
     *
     * <p>
     * 使用 {@link BaseServiceImpl#pageWithConverter} 实现，直接转换为 ListVO。
     * </p>
     *
     * @param query 查询参数
     * @return 分页结果
     */
    @Override
    public PageResult<ArticleListVO> pageList(ArticleQueryDTO query) {
        log.info("分页查询文章: current={}, size={}, categoryId={}, tagId={}, keyword={}",
                query.getCurrent(), query.getSize(), query.getCategoryId(),
                query.getTagId(), query.getKeyword());

        // 构造分页对象
        Page<ArticleEntity> page = new Page<>(query.getCurrent(), query.getSize());

        // 构建查询条件
        LambdaQueryWrapper<ArticleEntity> wrapper = buildQueryWrapper(query);

        // 使用 BaseServiceImpl 的 pageWithConverter（直接 Entity -> ListVO）
        IPage<ArticleListVO> listVoPage = this.pageWithConverter(page, wrapper, converter::entityToListVo);

        // 转换为 PageResult
        return PageResult.of(listVoPage);
    }

    /**
     * 构建查询条件（抽取为私有方法，提高可维护性）
     */
    private LambdaQueryWrapper<ArticleEntity> buildQueryWrapper(ArticleQueryDTO query) {
        LambdaQueryWrapper<ArticleEntity> wrapper = new LambdaQueryWrapper<>();

        // 基础条件：未删除
        wrapper.eq(ArticleEntity::getIsDeleted, 0);

        // 状态筛选（默认只查已发布）
        if (query.getStatus() != null) {
            wrapper.eq(ArticleEntity::getStatus, query.getStatus());
        } else {
            // 用户端默认只查询已发布
            wrapper.eq(ArticleEntity::getStatus, ArticleStatus.PUBLISHED.getCode());
        }

        // 分类筛选
        if (query.getCategoryId() != null) {
            wrapper.eq(ArticleEntity::getCategoryId, query.getCategoryId());
        }

        // 作者筛选
        if (query.getAuthorId() != null) {
            wrapper.eq(ArticleEntity::getAuthorId, query.getAuthorId());
        }

        // 关键词搜索（标题 + 摘要）
        if (StringUtils.isNotBlank(query.getKeyword())) {
            wrapper.and(w -> w
                    .like(ArticleEntity::getTitle, query.getKeyword())
                    .or()
                    .like(ArticleEntity::getSummary, query.getKeyword()));
        }

        // 标签筛选（需要子查询）
        if (query.getTagId() != null) {
            wrapper.inSql(ArticleEntity::getId,
                    "SELECT article_id FROM art_article_tag WHERE tag_id = " + query.getTagId()
                            + " AND is_deleted = 0");
        }

        // 排序：置顶优先，然后按发布时间倒序
        wrapper.orderByDesc(ArticleEntity::getIsTop, ArticleEntity::getPublishTime);

        return wrapper;
    }
}

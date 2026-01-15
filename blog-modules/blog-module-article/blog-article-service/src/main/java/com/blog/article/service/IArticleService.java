package com.blog.article.service;

import com.blog.article.api.dto.ArticleDTO;
import com.blog.article.api.dto.ArticleQueryDTO;
import com.blog.article.api.vo.ArticleDetailVO;
import com.blog.article.api.vo.ArticleListVO;
import com.blog.article.domain.entity.ArticleEntity;
import com.blog.common.base.IBaseService;
import com.blog.common.model.PageResult;

import java.util.List;

/**
 * 文章服务接口
 *
 * <p>
 * 定义文章模块的核心业务方法。
 * </p>
 *
 * <p>
 * 说明：
 * </p>
 * <ul>
 * <li>继承 IBaseService 获取通用CRUD方法</li>
 * <li>定义文章特有的业务方法（发布、归档、推荐等）</li>
 * <li>此接口位于 blog-article-api，供其他模块调用</li>
 * </ul>
 *
 * @author liusxml
 * @since 1.1.0
 */
public interface IArticleService extends IBaseService<ArticleEntity, ArticleDetailVO, ArticleDTO> {

    /**
     * 发布文章
     *
     * <p>
     * 将草稿状态的文章发布，状态变更为"已发布"。
     * </p>
     *
     * <p>
     * 副作用：
     * </p>
     * <ul>
     * <li>触发 ArticlePublishedEvent 事件</li>
     * <li>设置 publish_time 字段</li>
     * <li>初始化统计数据（art_article_stats）</li>
     * <li>异步生成内容向量（embedding）</li>
     * </ul>
     *
     * @param articleId 文章ID
     * @throws com.blog.common.exception.BusinessException 如果文章不存在或状态不允许发布
     */
    void publishArticle(Long articleId);

    /**
     * 归档文章
     *
     * <p>
     * 将已发布的文章归档，不再显示在列表中。
     * </p>
     *
     * @param articleId 文章ID
     */
    void archiveArticle(Long articleId);

    /**
     * 恢复归档文章
     *
     * <p>
     * 将已归档的文章恢复为已发布状态。
     * </p>
     *
     * @param articleId 文章ID
     */
    void unarchiveArticle(Long articleId);

    /**
     * 获取相关文章推荐（基于向量搜索）
     *
     * <p>
     * 使用 MySQL 9.4 VECTOR 类型进行语义相似度计算。
     * </p>
     *
     * @param articleId 当前文章ID
     * @param limit     推荐数量
     * @return 相关文章列表
     */
    List<ArticleListVO> getRelatedArticles(Long articleId, Integer limit);

    /**
     * 增加浏览量
     *
     * <p>
     * 异步更新 art_article_stats 表，避免阻塞主线程。
     * </p>
     *
     * @param articleId 文章ID
     */
    void incrementViewCount(Long articleId);

    /**
     * 检查文章访问权限
     *
     * <p>
     * 验证用户是否有权访问文章（考虑密码保护、草稿状态等）。
     * </p>
     *
     * @param articleId 文章ID
     * @param password  访问密码（可选）
     * @return true 如果允许访问
     */
    boolean checkAccessPermission(Long articleId, String password);

    /**
     * 分页查询文章列表
     *
     * <p>
     * 根据查询条件分页获取文章列表，支持：
     * </p>
     * <ul>
     * <li>分类筛选</li>
     * <li>标签筛选</li>
     * <li>关键词搜索</li>
     * <li>状态筛选</li>
     * </ul>
     *
     * @param query 查询参数（包含分页和筛选条件）
     * @return 分页结果
     */
    PageResult<ArticleListVO> pageList(ArticleQueryDTO query);
}

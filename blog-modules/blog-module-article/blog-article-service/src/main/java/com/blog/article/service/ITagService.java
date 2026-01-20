package com.blog.article.service;

import com.blog.article.domain.entity.ArticleTagEntity;
import com.blog.article.dto.TagDTO;
import com.blog.article.vo.TagVO;
import com.blog.common.base.IBaseService;

import java.util.List;

/**
 * 标签服务接口
 *
 * @author liusxml
 * @since 1.7.0
 */
public interface ITagService extends IBaseService<ArticleTagEntity, TagVO, TagDTO> {

    /**
     * 批量创建标签
     *
     * @param names 标签名称列表
     * @return 创建的标签ID列表
     */
    List<Long> batchCreate(List<String> names);

    /**
     * 合并标签
     *
     * @param sourceTagId 源标签ID
     * @param targetTagId 目标标签ID
     */
    void mergeTags(Long sourceTagId, Long targetTagId);

    /**
     * 更新标签的文章数量
     *
     * @param tagId 标签ID
     */
    void updateArticleCount(Long tagId);
}

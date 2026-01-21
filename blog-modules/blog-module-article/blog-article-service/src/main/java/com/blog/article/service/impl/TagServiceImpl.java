package com.blog.article.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.blog.article.infrastructure.converter.TagConverter;
import com.blog.article.domain.entity.ArticleTagEntity;
import com.blog.article.domain.entity.ArticleTagRelationEntity;
import com.blog.article.dto.TagDTO;
import com.blog.article.infrastructure.mapper.ArticleTagMapper;
import com.blog.article.infrastructure.mapper.ArticleTagRelationMapper;
import com.blog.article.service.ITagService;
import com.blog.article.vo.TagVO;
import com.blog.common.base.BaseServiceImpl;
import com.blog.common.exception.BusinessException;
import com.blog.common.exception.SystemErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 标签服务实现
 *
 * @author liusxml
 * @since 1.7.0
 */
@Slf4j
@Service
public class TagServiceImpl
        extends BaseServiceImpl<ArticleTagMapper, ArticleTagEntity, TagVO, TagDTO, TagConverter>
        implements ITagService {

    private final ArticleTagMapper tagMapper;
    private final ArticleTagRelationMapper tagRelationMapper;

    public TagServiceImpl(TagConverter converter, ArticleTagMapper mapper, ArticleTagRelationMapper relationMapper) {
        super(converter);
        this.tagMapper = mapper;
        this.tagRelationMapper = relationMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Long> batchCreate(List<String> names) {
        if (names == null || names.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> tagIds = new ArrayList<>();

        for (String name : names) {
            if (StringUtils.isBlank(name)) {
                continue;
            }

            // 检查标签是否已存在
            ArticleTagEntity existing = tagMapper.selectOne(
                    Wrappers.lambdaQuery(ArticleTagEntity.class)
                            .eq(ArticleTagEntity::getName, name.trim()));

            if (existing != null) {
                tagIds.add(existing.getId());
            } else {
                // 创建新标签
                ArticleTagEntity tag = new ArticleTagEntity();
                tag.setName(name.trim());
                tag.setSlug(generateSlug(name.trim()));
                tag.setArticleCount(0);
                tagMapper.insert(tag);
                tagIds.add(tag.getId());
            }
        }

        return tagIds;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void mergeTags(Long sourceTagId, Long targetTagId) {
        if (sourceTagId.equals(targetTagId)) {
            throw new BusinessException(SystemErrorCode.PARAM_ERROR, "源标签和目标标签不能相同");
        }

        // 检查标签是否存在
        ArticleTagEntity sourceTag = tagMapper.selectById(sourceTagId);
        ArticleTagEntity targetTag = tagMapper.selectById(targetTagId);

        if (sourceTag == null || targetTag == null) {
            throw new BusinessException(SystemErrorCode.NOT_FOUND);
        }

        // 查询源标签的所有文章关联
        List<ArticleTagRelationEntity> relations = tagRelationMapper.selectList(
                Wrappers.lambdaQuery(ArticleTagRelationEntity.class)
                        .eq(ArticleTagRelationEntity::getTagId, sourceTagId));

        // 更新文章标签关联
        for (ArticleTagRelationEntity relation : relations) {
            // 检查目标标签是否已关联该文章
            Long existingCount = tagRelationMapper.selectCount(
                    Wrappers.lambdaQuery(ArticleTagRelationEntity.class)
                            .eq(ArticleTagRelationEntity::getArticleId, relation.getArticleId())
                            .eq(ArticleTagRelationEntity::getTagId, targetTagId));

            if (existingCount == 0) {
                // 更新为目标标签
                relation.setTagId(targetTagId);
                tagRelationMapper.updateById(relation);
            } else {
                // 已存在，删除重复关联
                tagRelationMapper.deleteById(relation.getId());
            }
        }

        // 删除源标签
        tagMapper.deleteById(sourceTagId);

        // 更新目标标签的文章数量
        updateArticleCount(targetTagId);
    }

    @Override
    public void updateArticleCount(Long tagId) {
        Long count = tagRelationMapper.selectCount(
                Wrappers.lambdaQuery(ArticleTagRelationEntity.class)
                        .eq(ArticleTagRelationEntity::getTagId, tagId));

        ArticleTagEntity tag = new ArticleTagEntity();
        tag.setId(tagId);
        tag.setArticleCount(count.intValue());
        tagMapper.updateById(tag);
    }

    @Override
    protected void preSave(ArticleTagEntity entity) {
        // 自动生成slug
        if (StringUtils.isBlank(entity.getSlug())) {
            entity.setSlug(generateSlug(entity.getName()));
        }

        // 初始化articleCount
        if (entity.getArticleCount() == null) {
            entity.setArticleCount(0);
        }
    }

    @Override
    protected void preUpdate(ArticleTagEntity entity) {
        // 如果name变化了，更新slug
        ArticleTagEntity existing = tagMapper.selectById(entity.getId());
        if (existing != null && !existing.getName().equals(entity.getName())) {
            if (StringUtils.isBlank(entity.getSlug())) {
                entity.setSlug(generateSlug(entity.getName()));
            }
        }
    }

    @Override
    public List<TagVO> listTags(String orderBy, Integer limit) {
        log.debug("获取标签列表: orderBy={}, limit={}", orderBy, limit);

        var queryWrapper = Wrappers.lambdaQuery(ArticleTagEntity.class);

        // 排序
        switch (StringUtils.defaultIfBlank(orderBy, "article_count")) {
            case "create_time":
                queryWrapper.orderByDesc(ArticleTagEntity::getCreateTime);
                break;
            case "name":
                queryWrapper.orderByAsc(ArticleTagEntity::getName);
                break;
            case "article_count":
            default:
                queryWrapper.orderByDesc(ArticleTagEntity::getArticleCount);
                break;
        }

        // 限制数量
        if (limit != null && limit > 0) {
            queryWrapper.last("LIMIT " + limit);
        }

        List<ArticleTagEntity> tags = tagMapper.selectList(queryWrapper);
        return tags.stream()
                .map(converter::entityToVo)
                .collect(Collectors.toList());
    }

    @Override
    public java.util.Optional<TagVO> getBySlug(String slug) {
        log.debug("根据 slug 获取标签: slug={}", slug);

        if (StringUtils.isBlank(slug)) {
            return java.util.Optional.empty();
        }

        ArticleTagEntity entity = tagMapper.selectOne(
                Wrappers.lambdaQuery(ArticleTagEntity.class)
                        .eq(ArticleTagEntity::getSlug, slug));

        if (entity == null) {
            return java.util.Optional.empty();
        }

        return java.util.Optional.of(converter.entityToVo(entity));
    }

    /**
     * 生成URL友好的slug
     */
    private String generateSlug(String name) {
        if (StringUtils.isBlank(name)) {
            return "";
        }

        return name.toLowerCase()
                .replaceAll("[^a-z0-9\\u4e00-\\u9fa5]+", "-")
                .replaceAll("^-+|-+$", "");
    }
}

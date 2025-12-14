package com.blog.article.infrastructure.converter;

import com.blog.article.domain.entity.ArticleEntity;
import com.blog.common.base.BaseConverter;
import com.blog.dto.ArticleDTO;
import com.blog.vo.ArticleDetailVO;
import com.blog.vo.ArticleListVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * 文章转换器
 *
 * <p>
 * 遵循项目规范：
 * </p>
 * <ul>
 * <li>使用 MapStruct 自动生成转换代码</li>
 * <li>配置 nullValuePropertyMappingStrategy = IGNORE</li>
 * <li>继承 BaseConverter 获得基础转换方法</li>
 * </ul>
 *
 * <p>
 * 特殊映射处理：
 * </p>
 * <ul>
 * <li>Entity → VO: Long ID 转 String（防止精度丢失）</li>
 * <li>VO 包含关联对象（Author/Category/Tags），需业务层手动填充</li>
 * </ul>
 *
 * @author blog-system
 * @since 1.1.0
 */
@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ArticleConverter extends BaseConverter<ArticleDTO, ArticleEntity, ArticleDetailVO> {

    /**
     * Entity 转 DetailVO
     * <p>
     * Long类型ID会自动转为String（Jackson配置）
     * </p>
     *
     * @param entity 实体
     * @return VO
     */
    @Override
    @Mapping(target = "author", ignore = true) // 需业务层填充
    @Mapping(target = "category", ignore = true) // 需业务层填充
    @Mapping(target = "tags", ignore = true) // 需业务层填充
    @Mapping(target = "stats", ignore = true) // 需业务层填充
    ArticleDetailVO entityToVo(ArticleEntity entity);

    /**
     * Entity 转 ListVO（用于列表查询）
     * <p>
     * authorName、categoryName、tags、统计数据需业务层填充
     * </p>
     *
     * @param entity 实体
     * @return 列表VO
     */
    @Mapping(target = "authorName", ignore = true) // 需业务层填充
    @Mapping(target = "categoryName", ignore = true) // 需业务层填充
    @Mapping(target = "tags", ignore = true) // 需业务层填充
    @Mapping(target = "viewCount", ignore = true) // 需业务层填充
    @Mapping(target = "likeCount", ignore = true) // 需业务层填充
    @Mapping(target = "commentCount", ignore = true) // 需业务层填充
    ArticleListVO entityToListVo(ArticleEntity entity);

    /**
     * Integer 转 Boolean 的默认映射方法
     * <p>
     * 0 = false, 非0 = true
     * </p>
     *
     * @param value Integer值
     * @return Boolean值
     */
    default Boolean map(Integer value) {
        if (value == null) {
            return false;
        }
        return value != 0;
    }
}

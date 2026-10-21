package com.blog.article.infrastructure.converter;

import com.blog.article.api.dto.ArticleDTO;
import com.blog.article.api.vo.ArticleDetailVO;
import com.blog.article.api.vo.ArticleListVO;
import com.blog.article.domain.entity.ArticleEntity;
import com.blog.common.base.BaseConverter;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

/**
 * 文章转换器
 *
 * <p>
 * 遵循项目规范：
 * </p>
 * <ul>
 * <li>使用 {@code @Mapper(config = BaseConverter.class)} 复用全局 MapStruct 配置</li>
 * <li>使用 {@code @Named} + {@code qualifiedByName} 替代字符串表达式映射，保障编译期类型安全</li>
 * <li>继承 {@link BaseConverter} 获得通用转换方法</li>
 * </ul>
 *
 * <p>
 * 特殊映射处理：
 * </p>
 * <ul>
 * <li>Entity → VO: Long ID 转 String（防止前端精度丢失）</li>
 * <li>VO 包含关联对象（Author/Category/Tags），需业务层手动填充</li>
 * </ul>
 *
 * @author liusxml
 * @since 1.1.0
 */
@Mapper(config = BaseConverter.class)
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
    @Mapping(target = "status", source = "status", qualifiedByName = "mapStatus") // Integer -> String
    @Mapping(target = "createdAt", source = "createTime") // createTime -> createdAt
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

    /**
     * 状态码转状态字符串（通过 {@code @Named("mapStatus")} 供 {@code qualifiedByName} 类型安全调用）
     * <p>
     * 0-DRAFT, 2-PUBLISHED, 3-ARCHIVED
     * </p>
     *
     * @param statusCode 状态码
     * @return 状态字符串
     */
    @Named("mapStatus")
    default String mapStatus(Integer statusCode) {
        return switch (statusCode) {
            case 2 -> "PUBLISHED";
            case 3 -> "ARCHIVED";
            case null, default -> "DRAFT";
        };
    }
}

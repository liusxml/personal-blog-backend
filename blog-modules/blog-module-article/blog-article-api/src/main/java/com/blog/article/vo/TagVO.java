package com.blog.article.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 标签VO
 * 用于返回标签信息
 *
 * @author liusxml
 * @since 1.7.0
 */
@Data
@Schema(description = "标签VO")
public class TagVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "标签ID")
    private Long id;

    @Schema(description = "标签名称")
    private String name;

    @Schema(description = "URL标识")
    private String slug;

    @Schema(description = "标签颜色")
    private String color;

    @Schema(description = "标签描述")
    private String description;

    @Schema(description = "关联文章数量")
    private Integer articleCount;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}

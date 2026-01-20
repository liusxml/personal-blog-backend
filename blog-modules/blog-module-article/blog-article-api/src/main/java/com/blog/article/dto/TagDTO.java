package com.blog.article.dto;

import com.blog.common.base.Identifiable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 标签DTO
 * 用于创建和更新标签
 *
 * @author liusxml
 * @since 1.7.0
 */
@Data
@Schema(description = "标签DTO")
public class TagDTO implements Serializable, Identifiable<Long> {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "标签ID（更新时必填）")
    private Long id;

    @Schema(description = "标签名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "Spring Boot")
    private String name;

    @Schema(description = "URL标识（可选，自动生成）", example = "spring-boot")
    private String slug;

    @Schema(description = "标签颜色（HEX格式）", example = "#10B981")
    private String color;

    @Schema(description = "标签描述", example = "Spring Boot框架")
    private String description;

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}

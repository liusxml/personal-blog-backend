package com.blog.file.infrastructure.converter;

import com.blog.common.base.BaseConverter;
import com.blog.file.api.dto.FileDTO;
import com.blog.file.api.vo.FileVO;
import com.blog.file.domain.entity.FileEntity;
import org.mapstruct.Mapper;

/**
 * 文件转换器
 *
 * <p>
 * 遵循项目规范：
 * <ul>
 * <li>使用 {@code @Mapper(config = BaseConverter.class)} 复用全局 MapStruct 配置</li>
 * <li>继承 {@link BaseConverter} 获得基础转换方法</li>
 * </ul>
 * </p>
 *
 * @author liusxml
 * @since 1.0.0
 */
@Mapper(config = BaseConverter.class)
public interface FileConverter extends BaseConverter<FileDTO, FileEntity, FileVO> {
    // MapStruct 会自动实现 BaseConverter 的所有方法
    // 无需手动覆盖，避免 Ambiguous mapping 错误
}

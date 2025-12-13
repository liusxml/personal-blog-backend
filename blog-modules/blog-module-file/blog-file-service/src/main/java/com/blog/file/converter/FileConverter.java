package com.blog.file.converter;

import com.blog.common.base.BaseConverter;
import com.blog.dto.FileDTO;
import com.blog.file.entity.FileFile;
import com.blog.vo.FileVO;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * 文件转换器
 * 
 * <p>
 * 遵循项目规范：
 * <ul>
 * <li>使用 MapStruct 自动生成转换代码</li>
 * <li>配置 nullValuePropertyMappingStrategy = IGNORE</li>
 * <li>继承 BaseConverter 获得基础转换方法</li>
 * </ul>
 * </p>
 *
 * @author liusxml
 * @since 1.0.0
 */
@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface FileConverter extends BaseConverter<FileDTO, FileFile, FileVO> {
    // MapStruct 会自动实现 BaseConverter 的所有方法
    // 无需手动覆盖，避免 Ambiguous mapping 错误
}

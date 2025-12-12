package com.blog.infrastructure.storage;

import com.blog.common.exception.BusinessException;
import com.blog.enums.FileErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * 存储策略上下文（工厂）
 * <p>
 * 负责根据配置类型获取对应策略。
 * </p>
 */
@Component
@RequiredArgsConstructor
public class StorageContext {

    private final Map<String, FileStorageStrategy> strategyMap;

    /**
     * 获取存储策略
     *
     * @param type 存储类型（如 "BITIFUL"）
     * @return 对应策略实现
     * @throws BusinessException 类型不支持
     */
    public FileStorageStrategy getStrategy(String type) {
        return Optional.ofNullable(strategyMap.get(type.toUpperCase()))
                .orElseThrow(() -> new BusinessException(
                        FileErrorCode.FILE_STORAGE_TYPE_INVALID,
                        "不支持的存储类型: " + type
                ));
    }
}
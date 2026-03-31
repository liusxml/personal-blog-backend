package com.blog.ai.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.ai.domain.entity.OpsCiEventEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * Ops CI 事件 Mapper
 *
 * <p>
 * 继承 {@link BaseMapper} 获得完整 CRUD 能力（insert / selectById / updateById / deleteById 等）。
 * 无需额外配置，MyBatis-Plus 自动扫描注册。
 * </p>
 *
 * @author liusxml
 * @since 1.3.0
 */
@Mapper
public interface OpsCiEventMapper extends BaseMapper<OpsCiEventEntity> {
    // 当前阶段使用 BaseMapper 提供的标准方法即可，无需自定义查询
}

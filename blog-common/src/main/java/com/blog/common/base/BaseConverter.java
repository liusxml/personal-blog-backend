package com.blog.common.base;

import org.mapstruct.MapperConfig;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * 基础转换器接口与 MapStruct 全局配置中心，用于 DTO/Entity/VO 之间转换。
 * <p>
 * 通过 {@link MapperConfig} 统一收敛所有子模块转换器的公共配置：
 * </p>
 * <ul>
 *   <li>{@code componentModel = MappingConstants.ComponentModel.SPRING}：生成的实现类自动标注 {@code @Component}，支持 Spring 依赖注入</li>
 *   <li>{@code nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE}：局部更新时忽略 DTO 中的 {@code null} 属性，防止覆盖数据库已有值</li>
 *   <li>{@code unmappedTargetPolicy = ReportingPolicy.IGNORE}：忽略未映射的目标字段（如 {@code BaseEntity} 的审计字段 {@code createTime}、{@code version} 等），消除编译期冗余警告</li>
 * </ul>
 * <p>
 * 子接口只需声明 {@code @Mapper(config = BaseConverter.class)} 即可继承全部配置与基础转换方法。
 * </p>
 *
 * @param <D> DTO 类型。
 * @param <E> Entity 类型。
 * @param <V> VO 类型。
 * @since 1.0
 */
@MapperConfig(
        componentModel = MappingConstants.ComponentModel.SPRING,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface BaseConverter<D, E, V> {

    /**
     * 将 DTO 转换为 Entity，支持自定义映射表达式。
     *
     * @param dto DTO 对象，可为 null（返回 null）。
     * @return Entity 对象。
     */
    E dtoToEntity(D dto);

    /**
     * 将 Entity 转换为 VO，忽略敏感字段。
     *
     * @param entity Entity 对象，可为 null。
     * @return VO 对象。
     */
    V entityToVo(E entity);

    /**
     * 将 DTO 列表转换为 Entity 列表。
     *
     * @param dtoList DTO 列表，可为空。
     * @return Entity 列表。
     */
    List<E> dtoListToEntityList(List<D> dtoList);

    /**
     * 将 Entity 列表转换为 VO 列表。
     *
     * @param entityList Entity 列表。
     * @return VO 列表。
     */
    List<V> entityListToVoList(List<E> entityList);

    /**
     * 将 DTO 的非空属性增量更新到已存在的 Entity 上。
     * <p>
     * 显式配置 {@code @Mapping(target = "id", ignore = true)}，防止前端传入的 DTO 篡改持久化实体的主键 ID。
     * </p>
     *
     * @param dto    源 DTO 对象，包含需要更新的字段。
     * @param entity 目标 Entity 对象（从数据库查出的持久化对象）。
     */
    @Mapping(target = "id", ignore = true)
    void updateEntityFromDto(D dto, @MappingTarget E entity);
}
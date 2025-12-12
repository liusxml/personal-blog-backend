package com.blog.common.base;

import org.mapstruct.MappingTarget;

import java.util.List;

/**
 * 基础转换器接口，用于 DTO/Entity/VO 之间转换。
 * <p>
 * 设计为 MapStruct 实现，提供零配置映射。支持 Spring Boot 3 依赖注入。
 * 方法确保 null 安全和批量效率。
 * <p>
 * ⚠️ 最佳实践：
 * 实现类应添加
 * {@code @Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)}
 * 以确保 {@link #updateEntityFromDto(Object, Object)} 执行时，DTO 中的 null 值不会覆盖 Entity
 * 中的现有值。
 *
 * @param <D> DTO 类型。
 * @param <E> Entity 类型。
 * @param <V> VO 类型。
 * @since 1.0
 */
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
     * 将 DTO 列表转换为 Entity 列表，支持并行处理 (Java 21 streams)。
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
     * 【新增】将 DTO 的属性更新到已存在的 Entity 上。
     * 这是实现安全更新操作的核心，避免了全量覆盖。
     * 在 MapStruct 实现类中，必须使用 @MappingTarget 注解标记 'entity' 参数。
     *
     * @param dto    源 DTO 对象，包含需要更新的字段。
     * @param entity 目标 Entity 对象 (从数据库查出的持久化对象)。
     */
    void updateEntityFromDto(D dto, @MappingTarget E entity);
}
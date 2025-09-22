package com.blog.common.base;

import java.util.List;

/**
 * 基础转换器接口 (DTO, Entity, VO)
 * <p>
 * 通过 MapStruct 实现该接口，为 Service 层提供类型安全的对象转换。
 *
 * @param <D> DTO (Data Transfer Object) - 用于数据创建/更新的传入对象
 * @param <E> Entity (Entity - Persistent Object) - 数据库持久化对象
 * @param <V> VO (View Object) - 用于数据展示的传出对象
 */
public interface BaseConverter<D, E, V> {

    /**
     * 将 DTO 转换为 Entity
     *
     * @param dto DTO 对象
     * @return Entity 对象
     */
    E dtoToEntity(D dto);

    /**
     * 将 Entity 转换为 VO
     *
     * @param entity Entity 对象
     * @return VO 对象
     */
    V entityToVo(E entity);

    /**
     * 将 DTO 列表转换为 Entity 列表
     *
     * @param dtoList DTO 对象列表
     * @return Entity 对象列表
     */
    List<E> dtoListToEntityList(List<D> dtoList);

    /**
     * 将 Entity 列表转换为 VO 列表
     *
     * @param entityList Entity 对象列表
     * @return VO 对象列表
     */
    List<V> entityListToVoList(List<E> entityList);
}

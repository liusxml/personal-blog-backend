package com.blog.common.base;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

/**
 * 基础 Service 实现类
 *
 * @param <M> Mapper 类型
 * @param <E> Entity 类型
 * @param <V> VO 类型
 * @param <D> DTO 类型
 * @param <C> Converter 类型
 */
public abstract class BaseServiceImpl<M extends BaseMapper<E>, E, V, D, C extends BaseConverter<D, E, V>>
        extends ServiceImpl<M, E> implements IBaseService<E, V, D> {

    protected final C converter;
    public BaseServiceImpl(C converter) {
        this.converter = converter;
    }


    @Override
    public Optional<V> getVoById(Serializable id) {
        E entity = this.getById(id);
        return Optional.ofNullable(entity)
                .map(converter::entityToVo);
    }

    @Override
    public List<V> listVo(Wrapper<E> queryWrapper) {
        List<E> entities = this.list(queryWrapper);
        return converter.entityListToVoList(entities);
    }

    @Override
    public IPage<V> pageVo(Page<E> page, Wrapper<E> queryWrapper) {
        IPage<E> entityPage = this.page(page, queryWrapper);
        return entityPage.convert(converter::entityToVo);
    }

    @Override
    public boolean saveByDto(D dto) {
        E entity = converter.dtoToEntity(dto);
        // 这里可以加入通用逻辑，例如设置创建人、创建时间等
        // if (entity instanceof BaseEntity) { ... }
        return this.save(entity);
    }

    @Override
    public boolean updateByDto(D dto) {
        E entity = converter.dtoToEntity(dto);
        // 这里可以加入通用逻辑，例如设置更新人、更新时间等
        return this.updateById(entity);
    }
}

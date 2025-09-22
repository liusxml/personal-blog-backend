package com.blog.common.base;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

/**
 * 基础 Service 接口，在 Mybatis-Plus IService 基础上增加通用方法
 *
 * @param <E> Entity 类型
 * @param <V> VO 类型
 * @param <D> DTO 类型
 */
public interface IBaseService<E, V, D> extends IService<E> {

    /**
     * 根据 ID 查询并转换为 VO
     *
     * @param id 主键ID
     * @return 包装在 Optional 中的 VO 对象
     */
    Optional<V> getVoById(Serializable id);

    /**
     * 根据查询条件查询列表并转换为 VO 列表
     *
     * @param queryWrapper 查询条件
     * @return VO 对象列表
     */
    List<V> listVo(Wrapper<E> queryWrapper);

    /**
     * 根据查询条件进行分页查询，并将结果转换为 VO 的分页对象
     *
     * @param page         分页参数
     * @param queryWrapper 查询条件
     * @return 包含 VO 的分页结果
     */
    IPage<V> pageVo(Page<E> page, Wrapper<E> queryWrapper);

    /**
     * 通过 DTO 保存实体
     *
     * @param dto DTO 对象
     * @return 是否保存成功
     */
    boolean saveByDto(D dto);

    /**
     * 通过 DTO 更新实体
     *
     * @param dto DTO 对象 (必须包含ID)
     * @return 是否更新成功
     */
    boolean updateByDto(D dto);
}


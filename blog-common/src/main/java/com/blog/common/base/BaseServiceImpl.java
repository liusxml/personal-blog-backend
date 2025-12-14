package com.blog.common.base;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.blog.common.exception.BusinessException;
import com.blog.common.exception.EntityNotFoundException;
import com.blog.common.exception.OperationFailedException;
import com.blog.common.exception.SystemErrorCode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 基础 Service 实现类，提供通用 CRUD 操作和类型转换。
 * <p>
 * 基于 MyBatis-Plus ServiceImpl 扩展，支持 Spring Boot 3 AutoConfiguration。
 * 集成 MapStruct 转换器，确保高效映射。支持 Java 21 模式匹配在实体处理中。
 * 添加了通用钩子，如预保存/更新逻辑（e.g., timestamps, auditing）。
 *
 * @param <M> Mapper 类型，必须扩展 BaseMapper。
 * @param <E> Entity 类型。
 * @param <V> VO 类型。
 * @param <D> DTO 类型。
 * @param <C> Converter 类型，必须实现 BaseConverter。
 * @see IBaseService
 * @see BaseConverter
 * @since 1.0
 */
public abstract class BaseServiceImpl<M extends BaseMapper<E>, E, V, D extends Identifiable<? extends Serializable>, C extends BaseConverter<D, E, V>>
        extends ServiceImpl<M, E> implements IBaseService<E, V, D> {

    protected final C converter;

    @Autowired(required = false)
    private Validator validator;

    protected BaseServiceImpl(C converter) {
        this.converter = converter;
    }

    /**
     * 校验 DTO 对象，如果校验失败则抛出 BusinessException。
     * 如果未配置 Validator Bean（例如测试环境），则跳过校验。
     *
     * @param dto 待校验的 DTO 对象
     */
    protected void validate(D dto) {
        if (validator == null) {
            return;
        }
        var violations = validator.validate(dto);
        if (!violations.isEmpty()) {
            ConstraintViolation<D> firstViolation = violations.iterator().next();
            throw new BusinessException(
                    SystemErrorCode.PARAM_ERROR,
                    firstViolation.getPropertyPath() + ": " + firstViolation.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<V> getVoById(Serializable id) {
        E entity = this.getById(id);
        return Optional.ofNullable(entity).map(converter::entityToVo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<V> listVo(Wrapper<E> queryWrapper) {
        List<E> entities = this.list(queryWrapper);
        return converter.entityListToVoList(entities);
    }

    @Override
    @Transactional(readOnly = true)
    public IPage<V> pageVo(Page<E> page, Wrapper<E> queryWrapper) {
        IPage<E> entityPage = this.page(page, queryWrapper);
        return entityPage.convert(converter::entityToVo);
    }

    @Override
    @Transactional(readOnly = true)
    public <R> IPage<R> pageWithConverter(Page<E> page, Wrapper<E> queryWrapper,
                                          Function<E, R> converterFunc) {
        Assert.notNull(page, "分页参数不能为空");
        Assert.notNull(converterFunc, "转换函数不能为空");

        IPage<E> entityPage = this.page(page, queryWrapper);
        return entityPage.convert(converterFunc);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Serializable saveByDto(D dto) {
        validate(dto);
        E entity = converter.dtoToEntity(dto);
        preSave(entity); // 执行预保存钩子
        if (!this.save(entity)) {
            // 可以定义一个更具体的异常，如 SaveFailedException
            throw new OperationFailedException("实体保存失败", entity);
        }
        // 使用通用的 getEntityId 方法返回主键
        return getEntityId(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateByDto(D dto) {
        validate(dto);
        // 1. 从 DTO 中自动获取主键 ID
        Serializable id = getEntityIdFromDto(dto);
        Assert.notNull(id, "更新失败：无法从 DTO 中获取主键 ID。");
        // 2. 从数据库加载原始 Entity
        E entity = this.getById(id);
        if (entity == null) {
            throw new EntityNotFoundException(this.getEntityClass().getSimpleName(), id);
        }
        // 3. 【关键】调用新的转换方法，将 DTO 的属性增量更新到已存在的 entity 对象上
        converter.updateEntityFromDto(dto, entity);
        // 4. 执行预更新钩子
        preUpdate(entity);
        // 5. 执行更新
        boolean success = this.updateById(entity);
        if (!success) {
            // 可能是由乐观锁 @Version 导致的更新失败
            throw new OperationFailedException("实体更新失败，可能存在数据冲突", dto);
        }
        return true;
    }

    @Override
    public boolean removeById(Serializable id) {
        return super.removeById(id);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public List<Serializable> batchSaveByDto(List<D> dtoList) {
        if (CollectionUtils.isEmpty(dtoList)) {
            return Collections.emptyList();
        }
        List<E> entities = converter.dtoListToEntityList(dtoList);
        entities.forEach(this::preSave);
        if (!this.saveBatch(entities)) {
            throw new OperationFailedException("实体批量保存失败");
        }
        return entities.stream().map(this::getEntityId).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateBatchByDto(List<D> dtoList) {
        if (CollectionUtils.isEmpty(dtoList)) {
            return true;
        }
        // ⚠️ 警告：
        // 1. 批量更新直接将 DTO 转为 Entity，不执行 "查 -> 改" 的增量更新逻辑。
        // 2. 如果 DTO 为部分字段，转换后的 Entity 中未赋值字段可能为 null。
        // 3. preUpdate 钩子拿到的 Entity 可能是不完整的（取决于 DTO 内容）。
        // 4. MyBatis-Plus 默认只更新非 null 字段，所以数据库数据是安全的，但内存逻辑需谨慎。

        // 1. 将 DTO 列表转换为 Entity 列表
        List<E> entities = converter.dtoListToEntityList(dtoList);
        // 2. 批量执行预更新钩子
        entities.forEach(this::preUpdate);
        // 3. 调用 MP 的批量更新方法
        boolean success = this.updateBatchById(entities);
        if (!success) {
            throw new OperationFailedException("实体批量更新失败", dtoList);
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeByIds(List<? extends Serializable> idList) {
        if (CollectionUtils.isEmpty(idList)) {
            return true;
        }
        // 注意：批量删除通常无法像单条删除一样精确抛出 EntityNotFoundException
        // 因为 MP 的 removeByIds 返回的是一个布尔值，而不是影响的行数。
        // 这里的实现是直接透传 MP 的行为。
        return super.removeByIds(idList);
    }

    // 注意：流式查询的实现需要自定义 Mapper 方法，因为它依赖 @Options 注解。
    // 因此，BaseServiceImpl 无法提供通用实现，但可以提供一个模板或抛出异常来提示子类实现。
    @Override
    public void streamVo(Wrapper<E> queryWrapper, StreamProcessor<V> processor) {
        // 流式查询需要 Mapper 层面的特殊支持，无法在 ServiceImpl 中通用实现。
        // 子类如果需要此功能，应重写此方法并调用自定义的流式查询 Mapper 方法。
        throw new UnsupportedOperationException("Stream query must be implemented in the specific service and mapper.");

        /*
         * 子类中的实现示例：
         *
         * @Override
         *
         * @Transactional(readOnly = true)
         * public void streamVo(Wrapper<User> queryWrapper, StreamProcessor<UserVO>
         * processor) {
         * // 假设 userMapper 中有一个 streamList(Wrapper) 方法
         * try (Stream<User> entityStream = baseMapper.streamList(queryWrapper)) {
         * processor.process(entityStream.map(converter::entityToVo));
         * }
         * }
         *
         * 对应的 UserMapper.java:
         *
         * @Select("SELECT * FROM user")
         *
         * @Options(resultSetType = ResultSetType.FORWARD_ONLY, fetchSize = 1000)
         *
         * @ResultType(User.class)
         * Stream<User> streamList(@Param(Constants.WRAPPER) Wrapper<User> wrapper);
         */
    }

    /**
     * 通用的实体 ID 获取方法，无需子类实现
     */
    protected Serializable getEntityId(E entity) {
        TableInfo tableInfo = TableInfoHelper.getTableInfo(this.getEntityClass());
        Assert.notNull(tableInfo, "无法获取 " + this.getEntityClass().getName() + " 的实体表信息。");
        // 关键改动：使用 TableInfo 自带的 getPropertyValue 方法
        Assert.hasText(tableInfo.getKeyProperty(), "无法获取 " + this.getEntityClass().getName() + " 的主键属性。");
        return (Serializable) tableInfo.getPropertyValue(entity, tableInfo.getKeyProperty());
    }

    /**
     * 通用的从 DTO 获取 ID 的辅助方法
     */
    private Serializable getEntityIdFromDto(D dto) {
        Assert.notNull(dto, "DTO 对象不能为空。");
        return dto.getId();
    }

    /**
     * 预保存钩子，可覆盖以添加自定义逻辑（如设置创建人/时间）。
     *
     * @param entity 待保存实体。
     */
    protected void preSave(E entity) {
        // 示例：设置创建时间
    }

    /**
     * 预更新钩子，可覆盖。
     *
     * @param entity 待更新实体。
     */
    protected void preUpdate(E entity) {
        // 示例：使用 Java 21 模式匹配检查版本
    }
}
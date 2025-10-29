package com.blog.common.base;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.blog.common.exception.EntityNotFoundException;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

/**
 * 基础 Service 接口，在 MyBatis-Plus IService 基础上增加通用方法，支持 RESTful API CRUD 操作。
 * <p>
 * 该接口提供类型安全的 DTO/Entity/VO 转换方法，适用于 Spring Boot 3 环境。
 * 方法设计符合 RESTful API 原则：get/list/page 为读取 (GET)，save/update 为修改 (POST/PUT)，remove 为删除 (DELETE)。
 * 所有方法均为线程安全，支持 Java 21 的虚拟线程执行。
 *
 * @param <E> Entity 类型 - 数据库持久化对象，必须实现 Serializable 并可选标注 @Version/@TableLogic。
 * @param <V> VO 类型 - 用于响应输出的视图对象，建议使用 record (Java 21+) 定义简单不可变结构。
 * @param <D> DTO 类型 - 用于请求输入的数据传输对象，建议使用 @Validated 注解进行校验。
 * @see BaseServiceImpl
 * @see BaseConverter
 * @since 1.0
 */
public interface IBaseService<E, V, D extends Identifiable<? extends Serializable>> extends IService<E> {

    /**
     * 根据 ID 查询实体并转换为 VO，支持 RESTful API GET 操作。
     * <p>
     * 如果实体不存在，返回空 Optional，便于控制器返回 404 Not Found。
     *
     * @param id 主键 ID，必须为 Serializable 类型（如 Long 或 String）。
     * @return 包装在 Optional 中的 VO 对象，如果不存在则为空。
     * @throws IllegalArgumentException 如果 ID 为 null 或无效。
     * @implNote 使用 MyBatis-Plus getById 实现，结合 MapStruct 转换。
     * @since 1.0
     */
    Optional<V> getVoById(Serializable id);

    /**
     * 根据查询条件查询实体列表并转换为 VO 列表，支持 RESTful API GET 操作。
     * <p>
     * 适用于无分页的批量查询。查询条件可使用 LambdaQueryWrapper 以确保类型安全。
     *
     * @param queryWrapper 查询条件包装器，可为 null 表示无条件查询。
     * @return VO 对象列表，如果无结果则为空列表。
     * @throws RuntimeException 如果查询执行失败（如数据库连接问题）。
     * @implNote 使用 MyBatis-Plus list 实现，结合 MapStruct 批量转换。
     * @since 1.0
     */
    List<V> listVo(Wrapper<E> queryWrapper);

    /**
     * 根据查询条件进行分页查询，并将结果转换为 VO 的分页对象，支持 RESTful API GET 操作。
     * <p>
     * 分页参数包括当前页、页大小等。支持排序 via page.setOrders()。
     *
     * @param page         分页参数对象，MyBatis-Plus Page 类型。
     * @param queryWrapper 查询条件包装器，可为 null。
     * @return 包含 VO 的分页结果，包括总记录数、当前页数据等。
     * @throws IllegalArgumentException 如果 page 为 null 或参数无效。
     * @implNote 使用 MyBatis-Plus page 实现，并通过 IPage.convert() 应用转换。
     * @since 1.0
     */
    IPage<V> pageVo(Page<E> page, Wrapper<E> queryWrapper);

    /**
     * 通过 DTO 保存新实体，支持 RESTful API POST 操作。
     * <p>
     * DTO 转换为 Entity 后持久化。支持预保存钩子，如设置创建时间。
     *
     * @param dto DTO 对象，必须包含必要字段。
     * @return 保存后的实体 ID。
     * @throws IllegalArgumentException 如果 DTO 无效或转换失败。
     * @implNote 使用 MapStruct dtoToEntity 转换，MyBatis-Plus save 持久化，并回填 ID。
     * @since 1.0
     */
    Serializable saveByDto(D dto);

    /**
     * 通过 DTO 更新现有实体，支持 RESTful API PUT 操作。
     * <p>
     * DTO 必须包含 ID。支持乐观锁 via @Version。
     *
     * @param dto DTO 对象，必须包含 ID 和更新字段。
     * @return 是否更新成功。
     * @throws EntityNotFoundException 如果 ID 不存在。
     * @implNote 使用 MapStruct dtoToEntity 转换，MyBatis-Plus updateById。
     * @since 1.0
     */
    boolean updateByDto(D dto);

    /**
     * 根据 ID 删除实体，支持 RESTful API DELETE 操作。默认物理删除。
     * <p>
     * 如果实体使用 @TableLogic，则为逻辑删除。
     *
     * @param id 主键 ID。
     * @return 是否删除成功。
     * @throws EntityNotFoundException 如果 ID 不存在。
     * @implNote 使用 MyBatis-Plus removeById。
     * @since 1.0
     */
    boolean removeById(Serializable id);

    /**
     * 通过 DTO 列表批量保存实体，支持 RESTful API POST 批量操作。
     *
     * @param dtoList DTO 对象列表。
     * @return 保存后的实体 ID 列表。
     * @implNote 使用 MyBatis-Plus saveBatch，并回填每个 ID。
     * @since 1.0
     */
    List<Serializable> batchSaveByDto(List<D> dtoList);

    /**
     * 通过 DTO 列表批量更新实体。
     *
     * @param dtoList 包含 ID 和待更新字段的 DTO 列表。
     * @return 是否全部更新成功。
     * @since 1.0
     */
    boolean updateBatchByDto(List<D> dtoList);

    /**
     * 根据 ID 列表批量删除实体。
     *
     * @param idList ID 列表。
     * @return 是否全部删除成功。
     * @since 1.0
     */
    boolean removeByIds(List<? extends Serializable> idList);

    /**
     * 以流式方式处理查询结果，并将实体转换为 VO。
     * 适用于处理大数据量，避免 OOM。
     *
     * @param queryWrapper 查询条件
     * @param processor    流处理器
     */
    void streamVo(Wrapper<E> queryWrapper, StreamProcessor<V> processor);
}
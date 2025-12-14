package com.blog.common.model;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 分页响应结果
 *
 * <p>
 * 统一的分页响应格式，用于包装分页数据。与框架无关，隐藏底层实现细节。
 * </p>
 *
 * <p>
 * 使用示例：
 * </p>
 *
 * <pre>{@code
 * // 从 MyBatis-Plus Page 转换
 * Page<ArticleEntity> page = articleMapper.selectPage(...);
 * PageResult<ArticleListVO> result = PageResult.of(page);
 *
 * // 手动构建
 * PageResult<UserVO> result = new PageResult<>();
 * result.setRecords(userList);
 * result.setTotal(100L);
 * }</pre>
 *
 * @param <T> 数据类型
 * @author blog-system
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "分页响应结果")
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 数据列表
     */
    @Schema(description = "数据列表")
    private List<T> records;

    /**
     * 总记录数
     */
    @Schema(description = "总记录数", example = "100")
    private Long total;

    /**
     * 当前页码
     */
    @Schema(description = "当前页码", example = "1")
    private Long current;

    /**
     * 每页数量
     */
    @Schema(description = "每页数量", example = "10")
    private Long size;

    /**
     * 总页数
     */
    @Schema(description = "总页数", example = "10")
    private Long pages;

    /**
     * 从 MyBatis-Plus Page 对象转换
     *
     * <p>
     * 注意：records 保持原类型，如需转换请使用 {@link #of(Page, List)}
     * </p>
     *
     * @param page MyBatis-Plus 分页对象
     * @param <T>  数据类型
     * @return 分页结果
     */
    public static <T> PageResult<T> of(Page<T> page) {
        PageResult<T> result = new PageResult<>();
        result.setRecords(page.getRecords());
        result.setTotal(page.getTotal());
        result.setCurrent(page.getCurrent());
        result.setSize(page.getSize());
        result.setPages(page.getPages());
        return result;
    }

    /**
     * 从 MyBatis-Plus Page 对象转换（带类型转换）
     *
     * <p>
     * 适用于需要将 Entity 转换为 VO 的场景
     * </p>
     *
     * @param page    MyBatis-Plus 分页对象
     * @param records 转换后的数据列表
     * @param <T>     目标数据类型
     * @param <E>     原始数据类型
     * @return 分页结果
     */
    public static <T, E> PageResult<T> of(Page<E> page, List<T> records) {
        PageResult<T> result = new PageResult<>();
        result.setRecords(records);
        result.setTotal(page.getTotal());
        result.setCurrent(page.getCurrent());
        result.setSize(page.getSize());
        result.setPages(page.getPages());
        return result;
    }

    /**
     * 从 MyBatis-Plus IPage 对象转换
     *
     * <p>
     * 适用于泛型分页查询结果（如 pageWithConverter 返回的 IPage）
     * </p>
     *
     * @param page IPage 分页对象
     * @param <T>  数据类型
     * @return 分页结果
     * @since 1.1.0
     */
    public static <T> PageResult<T> of(com.baomidou.mybatisplus.core.metadata.IPage<T> page) {
        PageResult<T> result = new PageResult<>();
        result.setRecords(page.getRecords());
        result.setTotal(page.getTotal());
        result.setCurrent(page.getCurrent());
        result.setSize(page.getSize());
        result.setPages(page.getPages());
        return result;
    }

    /**
     * 创建空分页结果
     *
     * @param query 查询参数
     * @param <T>   数据类型
     * @return 空分页结果
     */
    public static <T> PageResult<T> empty(PageQuery query) {
        PageResult<T> result = new PageResult<>();
        result.setRecords(List.of());
        result.setTotal(0L);
        result.setCurrent(query.getCurrent());
        result.setSize(query.getSize());
        result.setPages(0L);
        return result;
    }
}

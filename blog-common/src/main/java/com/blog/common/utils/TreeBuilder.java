package com.blog.common.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * 通用树形结构构建器
 *
 * <p>
 * 支持任意类型的列表转换为树形结构
 * </p>
 *
 * @param <T>  节点类型
 * @param <ID> ID类型
 * @author liusxml
 * @since 1.2.1
 */
public class TreeBuilder<T, ID> {

    private final Function<T, ID> idGetter;
    private final Function<T, ID> parentIdGetter;
    private final BiConsumer<T, List<T>> childrenSetter;

    /**
     * 构造函数
     *
     * @param idGetter       获取节点ID的函数
     * @param parentIdGetter 获取父节点ID的函数
     * @param childrenSetter 设置子节点列表的函数
     */
    public TreeBuilder(
            Function<T, ID> idGetter,
            Function<T, ID> parentIdGetter,
            BiConsumer<T, List<T>> childrenSetter) {
        this.idGetter = idGetter;
        this.parentIdGetter = parentIdGetter;
        this.childrenSetter = childrenSetter;
    }

    /**
     * 构建树形结构（单根）
     *
     * @param nodes  扁平列表
     * @param rootId 根节点ID
     * @return 树形结构（单个根节点）
     */
    public T buildTree(List<T> nodes, ID rootId) {
        if (nodes == null || nodes.isEmpty()) {
            return null;
        }

        List<T> roots = buildForest(nodes);

        return roots.stream()
                .filter(node -> Objects.equals(idGetter.apply(node), rootId))
                .findFirst()
                .orElse(null);
    }

    /**
     * 构建树形结构（森林，多个根节点）
     *
     * @param nodes 扁平列表
     * @return 树形列表（根节点列表）
     */
    public List<T> buildForest(List<T> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return Collections.emptyList();
        }

        // 1. 按ID建立索引
        Map<ID, T> nodeMap = new HashMap<>(nodes.size());
        for (T node : nodes) {
            nodeMap.put(idGetter.apply(node), node);
        }

        // 2. 构建父子关系
        List<T> roots = new ArrayList<>();
        for (T node : nodes) {
            ID parentId = parentIdGetter.apply(node);

            if (parentId == null) {
                // 根节点
                roots.add(node);
            } else {
                // 子节点，添加到父节点的children
                T parent = nodeMap.get(parentId);
                if (parent != null) {
                    List<T> children = getOrCreateChildren(parent);
                    children.add(node);
                } else {
                    // 父节点不存在，当作根节点处理（数据不一致）
                    roots.add(node);
                }
            }
        }

        return roots;
    }

    /**
     * 获取或创建子节点列表
     */
    @SuppressWarnings("unchecked")
    private List<T> getOrCreateChildren(T parent) {
        // 通过反射获取children字段（简化处理）
        try {
            var field = parent.getClass().getDeclaredField("children");
            field.setAccessible(true);
            List<T> children = (List<T>) field.get(parent);
            if (children == null) {
                children = new ArrayList<>();
                childrenSetter.accept(parent, children);
            }
            return children;
        } catch (Exception e) {
            List<T> children = new ArrayList<>();
            childrenSetter.accept(parent, children);
            return children;
        }
    }
}

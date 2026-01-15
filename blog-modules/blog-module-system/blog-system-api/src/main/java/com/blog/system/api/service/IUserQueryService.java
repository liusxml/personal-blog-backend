package com.blog.system.api.service;

import java.util.Collection;
import java.util.List;

/**
 * 用户查询服务接口（供其他模块调用）
 *
 * @author liusxml
 * @since 1.6.0
 */
public interface IUserQueryService {

    /**
     * 根据用户名批量查询用户ID
     *
     * @param usernames 用户名集合
     * @return 用户ID列表
     */
    List<Long> getUserIdsByUsernames(Collection<String> usernames);
}

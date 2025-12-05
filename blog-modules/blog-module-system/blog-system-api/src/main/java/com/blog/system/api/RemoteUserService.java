package com.blog.system.api;

import com.blog.system.api.dto.UserDTO;

import java.util.List;

/**
 * 用户远程服务接口
 * <p>
 * 提供给其他模块调用的用户服务接口
 *
 * @author liusxml
 * @since 1.0.0
 */
public interface RemoteUserService {

    /**
     * 批量获取用户信息
     *
     * @param userIds 用户ID列表
     * @return 用户信息列表
     */
    List<UserDTO> getUsersByIds(List<Long> userIds);

    /**
     * 根据ID获取用户信息
     *
     * @param userId 用户ID
     * @return 用户信息，如果不存在则返回 null
     */
    UserDTO getUserById(Long userId);
}

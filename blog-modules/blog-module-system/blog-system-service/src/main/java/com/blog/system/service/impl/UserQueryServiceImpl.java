package com.blog.system.service.impl;

import com.blog.system.api.service.IUserQueryService;
import com.blog.system.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 用户查询服务实现
 *
 * @author liusxml
 * @since 1.6.0
 */
@Service
@RequiredArgsConstructor
public class UserQueryServiceImpl implements IUserQueryService {

    private final UserMapper userMapper;

    @Override
    public List<Long> getUserIdsByUsernames(Collection<String> usernames) {
        if (usernames == null || usernames.isEmpty()) {
            return Collections.emptyList();
        }
        return userMapper.selectUserIdsByUsernames(usernames);
    }
}

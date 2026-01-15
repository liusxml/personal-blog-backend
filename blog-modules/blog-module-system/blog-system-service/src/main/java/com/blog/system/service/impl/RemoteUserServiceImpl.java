package com.blog.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.common.utils.RedisUtils;
import com.blog.system.api.RemoteUserService;
import com.blog.system.api.dto.UserDTO;
import com.blog.system.converter.UserConverter;
import com.blog.system.domain.entity.UserEntity;
import com.blog.system.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 远程用户服务实现
 * <p>
 * 提供跨模块的用户信息查询服务，使用 Redis 缓存优化性能
 *
 * @author liusxml
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RemoteUserServiceImpl implements RemoteUserService {

    private static final String USER_CACHE_KEY_PREFIX = "user:detail:";
    private static final long USER_CACHE_TTL_MINUTES = 30;
    private final UserMapper userMapper;
    private final UserConverter userConverter;
    private final RedisUtils redisUtils;

    @Override
    public UserDTO getUserById(Long userId) {
        if (userId == null) {
            return null;
        }

        // 1. 从缓存获取
        String cacheKey = USER_CACHE_KEY_PREFIX + userId;
        Object cached = redisUtils.get(cacheKey);
        if (cached instanceof UserDTO userDTO) {
            log.debug("缓存命中: userId={}", userId);
            return userDTO;
        }

        // 2. 从数据库查询
        UserEntity user = userMapper.selectById(userId);
        if (user == null) {
            return null;
        }

        UserDTO userDTO = userConverter.entityToDto(user);

        // 3. 写入缓存（带随机 TTL，防止雪崩）
        redisUtils.setWithRandomTTL(cacheKey, userDTO,
                USER_CACHE_TTL_MINUTES, TimeUnit.MINUTES, 10); // ±10% 随机

        return userDTO;
    }

    @Override
    public List<UserDTO> getUsersByIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }

        // 1. 构建缓存键
        List<String> cacheKeys = userIds.stream()
                .map(id -> USER_CACHE_KEY_PREFIX + id)
                .toList();

        // 2. 批量从 Redis 获取 (MGET)
        log.debug("批量查询用户: userIds={}", userIds);
        List<Object> cachedValues = redisUtils.mGet(cacheKeys);

        // 3. 分离缓存命中和未命中的用户
        List<UserDTO> result = new ArrayList<>();
        List<Long> missedUserIds = new ArrayList<>();
        Map<String, Object> cacheToSet = new HashMap<>();

        for (int i = 0; i < userIds.size(); i++) {
            Object cached = cachedValues.get(i);
            if (cached instanceof UserDTO userDTO) {
                // 缓存命中
                result.add(userDTO);
            } else {
                // 缓存未命中
                missedUserIds.add(userIds.get(i));
            }
        }

        // 4. 批量从数据库加载未命中的用户
        if (!missedUserIds.isEmpty()) {
            log.debug("缓存未命中用户数: {}", missedUserIds.size());

            List<UserEntity> users = userMapper.selectList(
                    new LambdaQueryWrapper<UserEntity>()
                            .in(UserEntity::getId, missedUserIds));

            // 5. 转换并准备批量写入缓存
            for (UserEntity user : users) {
                UserDTO userDTO = userConverter.entityToDto(user);
                result.add(userDTO);

                // 准备批量写入缓存
                String cacheKey = USER_CACHE_KEY_PREFIX + user.getId();
                cacheToSet.put(cacheKey, userDTO);
            }

            // 6. 批量写入缓存 (MSET)
            if (!cacheToSet.isEmpty()) {
                redisUtils.mSet(cacheToSet);

                // 设置过期时间（需逐个设置，因为 MSET 不支持 TTL）
                cacheToSet.keySet().forEach(key -> redisUtils.expire(key, USER_CACHE_TTL_MINUTES, TimeUnit.MINUTES));

                log.debug("批量缓存用户数: {}", cacheToSet.size());
            }
        }

        return result;
    }
}

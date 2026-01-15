package com.blog.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.system.api.dto.UserDTO;
import com.blog.system.converter.UserConverter;
import com.blog.system.domain.entity.UserEntity;
import com.blog.system.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * RemoteUserServiceImpl 单元测试
 *
 * @author liusxml
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class RemoteUserServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserConverter userConverter;

    @InjectMocks
    private RemoteUserServiceImpl remoteUserService;

    @Test
    void should_return_empty_list_when_userIds_is_null() {
        // When
        List<UserDTO> result = remoteUserService.getUsersByIds(null);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void should_return_empty_list_when_userIds_is_empty() {
        // When
        List<UserDTO> result = remoteUserService.getUsersByIds(Collections.emptyList());

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void should_get_users_by_ids_success() {
        // Given
        List<Long> userIds = Arrays.asList(1L, 2L);

        UserEntity user1 = new UserEntity();
        user1.setId(1L);
        user1.setUsername("user1");

        UserEntity user2 = new UserEntity();
        user2.setId(2L);
        user2.setUsername("user2");

        List<UserEntity> users = Arrays.asList(user1, user2);

        UserDTO dto1 = new UserDTO();
        dto1.setId(1L);
        dto1.setUsername("user1");

        UserDTO dto2 = new UserDTO();
        dto2.setId(2L);
        dto2.setUsername("user2");

        when(userMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(users);
        when(userConverter.entityToDto(user1)).thenReturn(dto1);
        when(userConverter.entityToDto(user2)).thenReturn(dto2);

        // When
        List<UserDTO> result = remoteUserService.getUsersByIds(userIds);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(1).getId()).isEqualTo(2L);
        verify(userMapper).selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    void should_return_null_when_userId_is_null() {
        // When
        UserDTO result = remoteUserService.getUserById(null);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void should_return_null_when_user_not_found() {
        // Given
        when(userMapper.selectById(1L)).thenReturn(null);

        // When
        UserDTO result = remoteUserService.getUserById(1L);

        // Then
        assertThat(result).isNull();
        verify(userMapper).selectById(1L);
    }

    @Test
    void should_get_user_by_id_success() {
        // Given
        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setUsername("testuser");

        UserDTO dto = new UserDTO();
        dto.setId(1L);
        dto.setUsername("testuser");

        when(userMapper.selectById(1L)).thenReturn(user);
        when(userConverter.entityToDto(user)).thenReturn(dto);

        // When
        UserDTO result = remoteUserService.getUserById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUsername()).isEqualTo("testuser");
        verify(userMapper).selectById(1L);
        verify(userConverter).entityToDto(user);
    }
}

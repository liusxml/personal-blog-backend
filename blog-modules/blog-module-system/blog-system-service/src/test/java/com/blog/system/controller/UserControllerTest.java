package com.blog.system.controller;

import com.blog.common.security.JwtAuthenticationDetails;
import com.blog.system.api.dto.UserDTO;
import com.blog.system.api.vo.UserVO;
import com.blog.system.service.IUserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerTest extends BaseControllerTest {

    private void mockSecurityContext(Long userId, String... roles) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        JwtAuthenticationDetails details = new JwtAuthenticationDetails(request, userId);

        List<SimpleGrantedAuthority> authorities = Arrays.stream(roles)
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken("testuser",
                "password", authorities);
        authentication.setDetails(details);

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    void should_get_current_user_success() throws Exception {
        mockSecurityContext(1L, "USER");

        UserVO userVO = new UserVO();
        userVO.setId(1L);
        userVO.setUsername("testuser");

        when(userService.getVoById(1L)).thenReturn(Optional.of(userVO));

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.username").value("testuser"));
    }

    @Test
    void should_update_current_user_success() throws Exception {
        mockSecurityContext(1L, "USER");

        UserDTO userDTO = new UserDTO();
        userDTO.setUsername("testuser");
        userDTO.setNickname("NewNick");

        when(userService.updateByDto(any(UserDTO.class))).thenReturn(true);

        mockMvc.perform(put("/api/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_get_user_by_id_success_admin() throws Exception {
        UserVO userVO = new UserVO();
        userVO.setId(2L);
        userVO.setUsername("other");

        when(userService.getVoById(2L)).thenReturn(Optional.of(userVO));

        mockMvc.perform(get("/api/users/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(2));
    }

    @Test
    @WithMockUser(roles = "USER")
    void should_get_user_by_id_forbidden_user() throws Exception {
        mockMvc.perform(get("/api/users/2"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_update_user_admin_success() throws Exception {
        UserDTO userDTO = new UserDTO();
        userDTO.setUsername("testuser");
        userDTO.setNickname("AdminUpdate");

        when(userService.updateByDto(any(UserDTO.class))).thenReturn(true);

        mockMvc.perform(put("/api/users/2")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userDTO)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_delete_user_admin_success() throws Exception {
        when(userService.removeById(2L)).thenReturn(true);

        mockMvc.perform(delete("/api/users/2"))
                .andExpect(status().isOk());
    }
}

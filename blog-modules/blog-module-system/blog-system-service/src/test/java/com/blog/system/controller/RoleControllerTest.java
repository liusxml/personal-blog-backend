package com.blog.system.controller;

import com.blog.system.api.dto.RoleDTO;
import com.blog.system.api.vo.RoleVO;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RoleControllerTest extends BaseControllerTest {

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_create_role_success() throws Exception {
        RoleDTO roleDTO = new RoleDTO();
        roleDTO.setRoleName("ROLE_TEST");
        roleDTO.setRoleKey("TEST");

        when(roleService.saveByDto(any(RoleDTO.class))).thenReturn(1L);

        mockMvc.perform(post("/api/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roleDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(1));
    }

    @Test
    @WithMockUser(roles = "USER")
    void should_create_role_forbidden_for_user() throws Exception {
        RoleDTO roleDTO = new RoleDTO();
        roleDTO.setRoleName("ROLE_TEST");
        roleDTO.setRoleKey("TEST"); // Valid key

        mockMvc.perform(post("/api/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roleDTO)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_get_role_success() throws Exception {
        RoleVO roleVO = new RoleVO();
        roleVO.setId(String.valueOf(1L));
        roleVO.setRoleName("ROLE_ADMIN");

        when(roleService.getVoById(1L)).thenReturn(Optional.of(roleVO));

        mockMvc.perform(get("/api/roles/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.roleName").value("ROLE_ADMIN"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_update_role_success() throws Exception {
        RoleDTO roleDTO = new RoleDTO();
        roleDTO.setRoleName("ROLE_UPDATED");
        roleDTO.setRoleKey("ROLE_UPDATED"); // Valid roleKey

        when(roleService.updateByDto(any(RoleDTO.class))).thenReturn(true);

        mockMvc.perform(put("/api/roles/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roleDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_delete_role_success() throws Exception {
        when(roleService.removeById(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/roles/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_assign_role_to_user_success() throws Exception {
        when(roleService.assignRoleToUser(1L, 2L)).thenReturn(true);

        mockMvc.perform(post("/api/roles/2/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_remove_role_from_user_success() throws Exception {
        when(roleService.removeRoleFromUser(1L, 2L)).thenReturn(true);

        mockMvc.perform(delete("/api/roles/2/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }
}

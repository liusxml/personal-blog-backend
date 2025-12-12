package com.blog.system.controller;

import com.blog.system.TestBlogSystemApplication;
import com.blog.system.service.IRoleService;
import com.blog.system.service.IUserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Controller 测试基类
 *
 * @author liusxml
 * @since 1.0.0
 */
@SpringBootTest(classes = TestBlogSystemApplication.class)
@AutoConfigureMockMvc
public abstract class BaseControllerTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @MockitoBean
    protected IUserService userService;

    @MockitoBean
    protected IRoleService roleService;
}

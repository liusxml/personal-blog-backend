package com.blog.system.service;

import com.blog.common.base.IBaseService;
import com.blog.system.api.dto.LoginDTO;
import com.blog.system.api.dto.RegisterDTO;
import com.blog.system.api.dto.UserDTO;
import com.blog.system.api.vo.LoginVO;
import com.blog.system.api.vo.UserVO;
import com.blog.system.entity.SysUser;

/**
 * 用户服务接口
 *
 * @author liusxml
 * @since 1.0.0
 */
public interface IUserService extends IBaseService<SysUser, UserVO, UserDTO> {

    /**
     * 用户注册
     *
     * @param registerDTO 注册信息
     * @return 用户信息
     */
    UserVO register(RegisterDTO registerDTO);

    /**
     * 用户登录
     *
     * @param loginDTO 登录信息
     * @return 登录响应（包含Token和用户信息）
     */
    LoginVO login(LoginDTO loginDTO);

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户实体
     */
    SysUser getUserByUsername(String username);
}

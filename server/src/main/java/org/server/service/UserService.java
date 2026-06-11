package org.server.service;

import org.server.dto.UserDTO;
import org.server.vo.UserVO;
import org.server.entity.User;
import java.util.List;

/**
 * 用户模块业务层接口
 * 聚合了账号密码认证、常规CRUD以及云托管短信快捷认证
 */
public interface UserService {

    UserVO getUserInfoById(Long userId);
    /**
     * 账号密码登录
     */
    User login(String username, String rawPassword);

    UserVO updateUserInfo(Long userId, UserDTO.UpdateInfo updateInfo);

    void register(UserDTO.UserRegisterDTO registerDTO);
}
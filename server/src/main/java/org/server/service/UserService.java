package org.server.service;

import org.server.dto.UserDTO;
import org.server.entity.User;
import java.util.List;

/**
 * 用户模块业务层接口
 * 聚合了账号密码认证、常规CRUD以及云托管短信快捷认证
 */
public interface UserService {

    /**
     * 获取所有用户列表
     */
    List<UserDTO> listUsers();

    /**
     * 根据用户ID获取用户DTO信息
     */
    UserDTO getById(Long id);

    /**
     * 保存/新增用户基本信息
     */
    boolean saveUser(UserDTO userDTO);

    /**
     * 账号密码登录
     */
    User login(String username, String rawPassword);

    /**
     * ⭐ 新增：发送短信验证码（带本地限流防刷机制）
     * @param phonenumber 接收验证码的手机号
     */
    void sendSmsCode(String phonenumber);

    /**
     * ⭐ 新增：短信验证码快捷登录/注册
     * @param phonenumber 手机号
     * @param code 4位或6位验证码
     * @return 经过本地查验/自动注册后的完整 User 实体对象
     */
    User loginBySms(String phonenumber, String code);
}
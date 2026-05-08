package org.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.server.common.exception.BusinessException;
import org.server.entity.User;
import org.server.mapper.UserMapper;
import org.server.dto.UserDTO;
import org.server.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public User login(String username, String rawPassword) {
        // 1. 根据用户名从数据库查询用户
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUserName, username));

        // 使用自定义业务异常，传入对应的错误码
        if (user == null) {
            throw new BusinessException(401, "用户不存在");
        }

        // 2. 使用 BCrypt 进行密码比对
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new BusinessException(401, "账号或密码错误");
        }

        return user;
    }

    @Override
    public List<UserDTO> listUsers() {
        // selectList(null) 表示查询所有
        List<User> users = userMapper.selectList(null);

        return users.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    public UserDTO getById(Long id) {
        User u = userMapper.selectById(id);
        if (u == null) return null;
        return convertToDTO(u);
    }

    @Override
    public boolean saveUser(UserDTO dto) {
        User user = new User();
        user.setUserName(dto.getUserName());
        user.setNickName(dto.getNickName());
        user.setEmail(dto.getEmail());
        user.setPhonenumber(dto.getPhonenumber());

        // 注意：如果是新增用户，通常这里还需要设置一个初始加密密码
        // user.setPassword(passwordEncoder.encode("123456"));

        return userMapper.insert(user) > 0;
    }

    // 提取一个私有方法处理 DTO 转换，让代码更整洁
    private UserDTO convertToDTO(User u) {
        UserDTO dto = new UserDTO();
        dto.setUserId(u.getUserId());
        dto.setUserName(u.getUserName());
        dto.setNickName(u.getNickName());
        dto.setEmail(u.getEmail());
        dto.setPhonenumber(u.getPhonenumber());
        return dto;
    }
}
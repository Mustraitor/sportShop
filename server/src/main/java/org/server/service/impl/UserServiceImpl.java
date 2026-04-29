package org.server.service.impl;

import org.server.entity.User;
import org.server.mapper.UserMapper;
import org.server.dto.UserDTO;
import org.server.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public List<UserDTO> listUsers() {
        List<User> users = userMapper.selectList(null);

        return users.stream().map(u -> {
            UserDTO dto = new UserDTO();
            dto.setUserId(u.getUserId());
            dto.setUserName(u.getUserName());
            dto.setNickName(u.getNickName());
            dto.setEmail(u.getEmail());
            dto.setPhonenumber(u.getPhonenumber());
            return dto;
        }).toList();
    }

    @Override
    public UserDTO getById(Long id) {
        User u = userMapper.selectById(id);

        UserDTO dto = new UserDTO();
        dto.setUserId(u.getUserId());
        dto.setUserName(u.getUserName());
        dto.setNickName(u.getNickName());
        dto.setEmail(u.getEmail());
        dto.setPhonenumber(u.getPhonenumber());

        return dto;
    }

    @Override
    public boolean saveUser(UserDTO dto) {
        User user = new User();
        user.setUserName(dto.getUserName());
        user.setNickName(dto.getNickName());
        user.setEmail(dto.getEmail());
        user.setPhonenumber(dto.getPhonenumber());

        return userMapper.insert(user) > 0;
    }
}


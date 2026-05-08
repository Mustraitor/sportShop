package org.server.service;

import org.server.dto.UserDTO;
import java.util.List;
import org.server.entity.User;

public interface UserService {

    List<UserDTO> listUsers();

    UserDTO getById(Long id);

    boolean saveUser(UserDTO userDTO);
    User login(String username, String rawPassword);
}

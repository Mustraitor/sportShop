package org.server.service;

import org.server.dto.UserDTO;
import java.util.List;

public interface UserService {

    List<UserDTO> listUsers();

    UserDTO getById(Long id);

    boolean saveUser(UserDTO userDTO);
}

package org.server.controller;

import org.server.common.Result;
import org.server.dto.UserDTO;
import org.server.service.UserService;
import org.server.vo.UserVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/list")
    public Result<List<UserVO>> list() {

        List<UserDTO> dtoList = userService.listUsers();

        List<UserVO> voList = dtoList.stream().map(dto -> {
            UserVO vo = new UserVO();
            vo.setUserName(dto.getUserName());
            vo.setNickName(dto.getNickName());
            vo.setEmail(dto.getEmail());
//            vo.setDisplayName("欢迎 " + dto.getUserName());
            return vo;
        }).toList();

        return Result.success(voList);
    }
}

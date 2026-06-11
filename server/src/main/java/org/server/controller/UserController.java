package org.server.controller;

import org.server.common.Result;
import org.server.dto.UserDTO;
import org.server.service.UserService;
import org.server.utils.BaseContext;
import org.server.vo.UserVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 获取用户信息
     */
    @GetMapping("/info")
    public Result<UserVO> getUserInfo() {
        // 兜底测试：如果没传 userId，默认查 1 号用户
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            userId = 1L;
        }
        UserVO userVO = userService.getUserInfoById(userId);
        return Result.success(userVO);
    }
    @PutMapping("/info")
// 💡 加上 @Validated，如果校验失败，Spring 会直接抛出 MethodArgumentNotValidException 异常
    public Result<UserVO> updateUserInfo(@Validated @RequestBody UserDTO.UpdateInfo updateInfo) {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            userId = 1L;
        }
        UserVO updatedUserVO = userService.updateUserInfo(userId, updateInfo);
        Result<UserVO> result = Result.success(updatedUserVO);
        result.setMsg("用户信息修改成功");
        return result;
    }
}
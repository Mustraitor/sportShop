package org.server.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.server.common.Result;
import org.server.dto.UserDTO;
import org.server.entity.User;
import org.server.service.CartService;
import org.server.service.UserService;
import org.server.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserService userService;
    @Autowired
    private CartService cartService;

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> body) {

        String username = body.get("username");
        String password = body.get("password");

        // 接收 guestId
        String guestId = body.get("guestId");

        //  登录
        User loginUser = userService.login(username, password);

        //  生成 token
        String token = JwtUtil.generateToken(
                loginUser.getUserId(),
                loginUser.getUserName()
        );

        // 登录成功后合并购物车（关键！！！）
        cartService.mergeCartAfterLogin(loginUser.getUserId(), guestId);

        // 返回数据
        Map<String, Object> data = new HashMap<>();
        data.put("userId", loginUser.getUserId());
        data.put("userName", loginUser.getUserName());
        data.put("token", token);

        return Result.success(data);
    }
    /**
     * 用户注册接口
     * @param registerDTO 包含 username, password, confirmPassword
     */
    @PostMapping("/register")
    public Result<String> register(@RequestBody @Validated UserDTO.UserRegisterDTO registerDTO) {
        // 调用 Service 层的注册逻辑
        // 如果 Service 抛出 BusinessException，会被全局异常处理器捕获并返回给前端
        userService.register(registerDTO);

        return Result.success("注册成功");
    }
    @PostMapping("/logout")
    public Result<String> logout(HttpServletRequest request) {
        return Result.success("退出成功");
    }
}
package org.server.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.server.common.Result;
import org.server.entity.User;
import org.server.service.UserService;
import org.server.utils.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        User loginUser = userService.login(username, password);
        String token = JwtUtil.generateToken(loginUser.getUserId(), loginUser.getUserName());

        // 准备返回给前端的数据
        Map<String, Object> data = new HashMap<>();
        data.put("userId", loginUser.getUserId());
        data.put("userName", loginUser.getUserName());

        // 把 Token 给前端，前端会把它存入 localStorage
        data.put("token", token);

        return Result.success(data);
    }
    @PostMapping("/logout")
    public Result<String> logout(HttpServletRequest request) {
        return Result.success("退出成功");
    }
}
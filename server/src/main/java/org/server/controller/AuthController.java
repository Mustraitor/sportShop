package org.server.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.server.common.Result;
import org.server.dto.UserDTO;
import org.server.entity.User;
import org.server.service.CartService;
import org.server.service.UserService;
import org.server.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
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

        // ⭐ 新增：接收 guestId
        String guestId = body.get("guestId");

        // 1️⃣ 登录
        User loginUser = userService.login(username, password);

        // 2️⃣ 生成 token
        String token = JwtUtil.generateToken(
                loginUser.getUserId(),
                loginUser.getUserName()
        );

        // ⭐ 3️⃣ 登录成功后合并购物车（关键！！！）
        cartService.mergeCartAfterLogin(loginUser.getUserId(), guestId);

        // 4️⃣ 返回数据
        Map<String, Object> data = new HashMap<>();
        data.put("userId", loginUser.getUserId());
        data.put("userName", loginUser.getUserName());
        data.put("token", token);

        return Result.success(data);
    }
    /**
     * ⭐ 2. 新增：发送短信验证码
     * 路由: POST /auth/sms/send
     */
    @PostMapping("/sms/send")
    public Result<Void> sendSms(@RequestBody UserDTO.SmsSend request) {
        try {
            // 联动你写在 UserService 或 AuthServiceImpl 中的发送逻辑
            userService.sendSmsCode(request.getPhonenumber());
            return Result.success("验证码发送成功，请查收短信", null);
        } catch (Exception e) {
            return Result.error(400, e.getMessage());
        }
    }

    /**
     *  3. 验证码快捷登录/注册
     * 路由: POST /auth/sms/login
     * 数据结构严格对齐密码登录，返回 Map 容器，包含 token 供前端存入 localStorage
     */
    @PostMapping("/sms/login")
    public Result<Map<String, Object>> smsLogin(@RequestBody UserDTO.SmsLogin request) {
        try {
            // 调用业务层执行：阿里云核验 -> 本地查/增用户
            User loginUser = userService.loginBySms(request.getPhonenumber(), request.getCode());

            // 使用你现有的 JwtUtil 颁发 Token
            String token = JwtUtil.generateToken(loginUser.getUserId(), loginUser.getUserName());

            // 完美对齐原有登录接口的数据回传格式
            Map<String, Object> data = new HashMap<>();
            data.put("userId", loginUser.getUserId());
            data.put("userName", loginUser.getUserName());
            data.put("token", token);

            return Result.success("登录成功", data);
        } catch (Exception e) {
            return Result.error(400, e.getMessage());
        }
    }
    @PostMapping("/logout")
    public Result<String> logout(HttpServletRequest request) {
        return Result.success("退出成功");
    }
}
package org.server.interceptor;

import org.server.common.exception.BusinessException;
import org.server.utils.JwtUtil;
import io.jsonwebtoken.Claims;
//import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 1. 从 Header 中获取名为 Authorization 的数据
        String authHeader = request.getHeader("Authorization");

        // 2. 校验是否为空，或者是否以 "Bearer " 开头（行业规范）
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BusinessException(401, "未登录或凭证无效");
        }

        // 3. 截取真正的 Token 字符串（去掉 "Bearer " 这7个字符）
        String token = authHeader.substring(7);

        try {
            Claims claims = JwtUtil.parseToken(token);
            request.setAttribute("userId", claims.get("userId"));
            return true;
        } catch (Exception e) {
            throw new BusinessException(401, "登录已过期，请重新登录");
        }
    }
}
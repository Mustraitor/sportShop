package org.server.interceptor;

import org.server.utils.JwtUtil;
import org.server.utils.BaseContext;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return true;
        }

        try {
            String token = authHeader.substring(7);

            Claims claims = JwtUtil.parseToken(token);

            Long userId = Long.valueOf(claims.get("userId").toString());

            // 写入线程变量
            BaseContext.setCurrentId(userId);
            request.setAttribute("userId", userId);

        } catch (Exception e) {

            return true;
        }

        return true;
    }

    /**
     * 请求彻底结束时，一定要把口袋掏空，防止 Tomcat 线程复用导致的内存泄漏
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        BaseContext.removeCurrentId();
    }
}
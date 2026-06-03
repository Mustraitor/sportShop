package org.server.interceptor;

import org.server.common.exception.BusinessException; // 对应你 common.exception 包
import org.server.utils.JwtUtil;
import org.server.utils.BaseContext; // 🚀 引入我们的隐形口袋工具类
import io.jsonwebtoken.Claims;
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

        // 2. 校验是否为空，或者是否以 "Bearer " 开头
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BusinessException(401, "未登录或凭证无效");
        }

        // 3. 截取真正的 Token 字符串（去掉 "Bearer " 这7个字符）
        String token = authHeader.substring(7);

        try {
            // 4. 解析 Token
            Claims claims = JwtUtil.parseToken(token);

            // 5. 拿到当初你登录存进去的 userId
            Long userId = Long.valueOf(claims.get("userId").toString());

            // 6. 把用户 ID 塞进 BaseContext 口袋里！
            BaseContext.setCurrentId(userId);

            // 保留你原本的 request 存放，兼容你可能已经写好的其他老接口
            request.setAttribute("userId", userId);

            return true;
        } catch (Exception e) {
            throw new BusinessException(401, "登录已过期，请重新登录");
        }
    }

    /**
     * 请求彻底结束时，一定要把口袋掏空，防止 Tomcat 线程复用导致的内存泄漏
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        BaseContext.removeCurrentId();
    }
}
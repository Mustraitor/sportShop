package org.server.dto;

import lombok.Data;

@Data
public class UserDTO {

    private Long userId;
    private String userName;
    private String nickName;
    private String email;
    private String phonenumber;
    // ==================== 短信发送请求专用的参数容器 ====================
    @Data
    public static class SmsSend {
        private String phonenumber; // 接收验证码的手机号
    }

    // ==================== 短信登录注册请求专用的参数容器 ====================
    @Data
    public static class SmsLogin {
        private String phonenumber; // 手机号
        private String code;        // 4位或6位短信验证码
    }
}

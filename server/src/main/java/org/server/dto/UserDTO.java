package org.server.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UserDTO {

    private Long userId;
    private String userName;
    private String nickName;
    private String email;
    private String phonenumber;

    @Data
    public static class UpdateInfo {
        @NotBlank(message = "昵称不能为空")
        private String nickName;
        @NotBlank(message = "邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        private String email;
        private String avatar;
        @Pattern(regexp = "^[012]$", message = "性别选择不合法")
        private String sex;
    }
    @Data
    public static class UserRegisterDTO {
        @NotBlank(message = "用户名不能为空")
        private String username;

        @NotBlank(message = "密码不能为空")
        private String password;

        // 🌟 新增字段
        @NotBlank(message = "请确认密码")
        private String confirmPassword;

        private String phone;
    }
}

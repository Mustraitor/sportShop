package org.server.dto;
import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
@Data
public class SmsSendDTO {

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phoneNumber;

    // 可选：验证码用途，如 "register", "login", "reset_pwd" 等
    private String scene = "default";
}

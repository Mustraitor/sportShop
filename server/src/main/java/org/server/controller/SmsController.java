package org.server.controller;

import org.server.common.Result;
import org.server.dto.SmsSendDTO;
import org.server.dto.SmsVerifyDTO;
import org.server.service.SmsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/sms")
public class SmsController {

    @Autowired
    private SmsService smsService;

    @PostMapping("/send")
    public Result<Void> sendSms(@RequestBody @Validated SmsSendDTO dto) {
        try {
            smsService.sendSmsCode(dto);
            return Result.success(null);
        } catch (Exception e) {
            log.error("发送短信失败", e);
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/verify")
    public Result<Boolean> verifySmsCode(@RequestBody @Validated SmsVerifyDTO dto) {
        boolean isValid = smsService.verifyCode(dto.getPhoneNumber(), dto.getScene(), dto.getCode());
        if (isValid) {
            return Result.success(true);
        } else {
            return Result.error("验证码错误或已过期");
        }
    }
}
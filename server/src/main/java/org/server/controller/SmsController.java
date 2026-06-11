package org.server.controller;

import org.server.common.Result;
import org.server.dto.SmsSendDTO;
import org.server.service.SmsService;   // 注入接口
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/sms")
public class SmsController {

    @Autowired
    private SmsService smsService;   // 接口类型，实际注入的是 SmsServiceImpl

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
}
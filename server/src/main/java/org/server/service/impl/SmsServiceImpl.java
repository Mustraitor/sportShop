package org.server.service.impl;

import com.aliyun.sdk.service.dypnsapi20170525.AsyncClient;
import com.aliyun.sdk.service.dypnsapi20170525.models.SendSmsVerifyCodeRequest;
import com.aliyun.sdk.service.dypnsapi20170525.models.SendSmsVerifyCodeResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.server.dto.SmsSendDTO;
import org.server.service.SmsService;

import java.util.concurrent.TimeUnit;
import java.util.Random;
@Slf4j
@Service
public class SmsServiceImpl implements SmsService {

    @Autowired
    private AsyncClient smsAsyncClient;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Value("${aliyun.sms.sign-name}")
    private String signName;

    @Value("${aliyun.sms.template-code}")
    private String templateCode;

    // 验证码有效期（秒）
    private static final long CODE_EXPIRE_SECONDS = 300;  // 5分钟

    // 发送频率限制（秒）
    private static final long SEND_INTERVAL_SECONDS = 60; // 60秒内不可重复发送

    @Override
    public void sendSmsCode(SmsSendDTO dto) throws Exception {
        String phone = dto.getPhoneNumber().trim();
        String scene = dto.getScene();
        log.info("发送验证码：手机号={}, 场景={}", phone, scene);

        // 1. 频率限制防刷
        String freqKey = "sms:freq:" + phone + ":" + scene;
        Boolean hasKey = redisTemplate.hasKey(freqKey);
        if (Boolean.TRUE.equals(hasKey)) {
            throw new RuntimeException("发送太频繁，请稍后再试");
        }

        // 2. 生成6位随机验证码
        String code = String.format("%06d", new Random().nextInt(999999));

        // 3. 构造阿里云短信请求（模板参数需与你的模板完全一致）
        //    你的模板示例：{"code":"##code##","min":"5"}
        String templateParam = String.format("{\"code\":\"%s\",\"min\":\"5\"}", code);

        SendSmsVerifyCodeRequest request = SendSmsVerifyCodeRequest.builder()
                .signName(signName)
                .templateCode(templateCode)
                .phoneNumber(phone)
                .templateParam(templateParam)
                .build();

        // 4. 调用阿里云API（同步等待结果）
        SendSmsVerifyCodeResponse response = smsAsyncClient.sendSmsVerifyCode(request).get();

        // 5. 处理返回结果
        if (response.getBody().getCode() != null && "OK".equals(response.getBody().getCode())) {
            // 发送成功：验证码存入Redis，并设置有效期
            String codeKey = "sms:code:" + phone + ":" + scene;
            redisTemplate.opsForValue().set(codeKey, code, CODE_EXPIRE_SECONDS, TimeUnit.SECONDS);
            // 设置发送频率限制标记
            redisTemplate.opsForValue().set(freqKey, "1", SEND_INTERVAL_SECONDS, TimeUnit.SECONDS);
            log.info("短信发送成功，手机号：{}，验证码：{}", phone, code);
        } else {
            String errorMsg = response.getBody().getMessage();
            log.error("短信发送失败，手机号：{}，错误：{}", phone, errorMsg);
            throw new RuntimeException("短信发送失败：" + errorMsg);
        }
        log.info("短信发送成功，手机号={}，验证码={}，key=sms:code:{}:{}", phone, code, phone, scene);
    }

    @Override
    public boolean verifyCode(String phone, String scene, String inputCode) {
        String trimmedPhone = phone.trim();
        String codeKey = "sms:code:" + trimmedPhone + ":" + scene;
        log.info("验证验证码：key={}, 输入code={}", codeKey, inputCode);
        String savedCode = redisTemplate.opsForValue().get(codeKey);
        log.info("Redis中保存的验证码={}", savedCode);
        if (savedCode != null && savedCode.equals(inputCode.trim())) {
            redisTemplate.delete(codeKey);
            log.info("验证成功，已删除key");
            return true;
        }
        log.warn("验证失败：savedCode={}, inputCode={}", savedCode, inputCode);
        return false;
    }
}
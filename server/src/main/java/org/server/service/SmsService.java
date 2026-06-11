package org.server.service;

import org.server.dto.SmsSendDTO;

/**
 * 短信验证码业务接口
 */
public interface SmsService {

    /**
     * 发送短信验证码
     * @param dto 请求参数（手机号、场景）
     * @throws Exception 发送失败时抛出异常（业务异常由Controller处理）
     */
    void sendSmsCode(SmsSendDTO dto) throws Exception;

    /**
     * 校验验证码是否正确（一次性使用）
     * @param phone 手机号
     * @param scene 场景（如 login、register）
     * @param inputCode 用户输入的验证码
     * @return true=验证成功，false=验证失败
     */
    boolean verifyCode(String phone, String scene, String inputCode);
}
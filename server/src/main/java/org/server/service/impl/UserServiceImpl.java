package org.server.service.impl;

import com.aliyun.auth.credentials.Credential;
import com.aliyun.auth.credentials.provider.StaticCredentialProvider;
import com.aliyun.sdk.service.dypnsapi20170525.AsyncClient;
import com.aliyun.sdk.service.dypnsapi20170525.models.CheckSmsVerifyCodeRequest;
import com.aliyun.sdk.service.dypnsapi20170525.models.CheckSmsVerifyCodeResponse;
import com.aliyun.sdk.service.dypnsapi20170525.models.SendSmsVerifyCodeRequest;
import com.aliyun.sdk.service.dypnsapi20170525.models.SendSmsVerifyCodeResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import darabonba.core.client.ClientOverrideConfiguration;
import org.server.common.exception.BusinessException;
import org.server.dto.UserDTO;
import org.server.entity.User;
import org.server.mapper.UserMapper;
import org.server.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private StringRedisTemplate redisTemplate; // ⭐ 新增：注入 Redis 工具

    // 阿里云凭证与模板配置（替换为你控制台的真实数据）
    private final String ACCESS_KEY_ID = "你的AccessKeyID";
    private final String ACCESS_KEY_SECRET = "你的AccessKeySecret";

    /**
     * 辅助私有方法：初始化阿里云官方新版异步客户端
     */
    private AsyncClient createAsyncClient() {
        StaticCredentialProvider provider = StaticCredentialProvider.create(
                Credential.builder()
                        .accessKeyId(ACCESS_KEY_ID)
                        .accessKeySecret(ACCESS_KEY_SECRET)
                        .build()
        );
        return AsyncClient.builder()
                .region("cn-hangzhou")
                .credentialsProvider(provider)
                .overrideConfiguration(ClientOverrideConfiguration.create().setEndpointOverride("dypnsapi.aliyuncs.com"))
                .build();
    }

    @Override
    public User login(String username, String rawPassword) {
        // 1. 根据用户名从数据库查询用户
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUserName, username));

        // 使用自定义业务异常，传入对应的错误码
        if (user == null) {
            throw new BusinessException(401, "用户不存在");
        }

        // 2. 使用 BCrypt 进行密码比对
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new BusinessException(401, "账号或密码错误");
        }

        return user;
    }

    /**
     * ⭐ 新增：发送短信验证码（带本地1分钟防刷限流）
     */
    @Override
    public void sendSmsCode(String phonenumber) {
        // 1. 本地 Redis 安全防护：1分钟内不能重复点
        String limitKey = "sms:limit:" + phonenumber;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(limitKey))) {
            throw new BusinessException(400, "发送验证码太频繁，请60秒后再试");
        }

        try (AsyncClient client = createAsyncClient()) {
            // 2. 组装请求体，完全对齐官方新版 V2.0 规范
            SendSmsVerifyCodeRequest sendRequest = SendSmsVerifyCodeRequest.builder()
                    .signName("速通互联验证码")
                    .templateCode("100001")
                    .phoneNumber(phonenumber)
                    .templateParam("{\"code\":\"##code##\",\"min\":\"5\"}") // 触发云端自动生成验证码
                    .build();

            CompletableFuture<SendSmsVerifyCodeResponse> response = client.sendSmsVerifyCode(sendRequest);
            SendSmsVerifyCodeResponse resp = response.get();

            if (resp.getBody() == null || !"OK".equalsIgnoreCase(resp.getBody().getCode())) {
                String errorMsg = resp.getBody() != null ? resp.getBody().getMessage() : "第三方未知错误";
                throw new BusinessException(500, "短信发送失败: " + errorMsg);
            }

            // 3. 发送成功，记录本地限流标记，防止接口被刷爆
            redisTemplate.opsForValue().set(limitKey, "1", 60, TimeUnit.SECONDS);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(500, "发送验证码系统异常: " + e.getMessage());
        }
    }

    /**
     * ⭐ 新增：验证码快捷登录/注册
     * 校验通过后，如果手机号不存在会自动执行 INSERT 注册
     */
    @Override
    public User loginBySms(String phonenumber, String code) {
        try (AsyncClient client = createAsyncClient()) {
            // 1. 组装核验请求，直接丢给阿里云比对
            CheckSmsVerifyCodeRequest checkRequest = CheckSmsVerifyCodeRequest.builder()
                    .phoneNumber(phonenumber)
                    .verifyCode(code)
                    .build();

            CompletableFuture<CheckSmsVerifyCodeResponse> response = client.checkSmsVerifyCode(checkRequest);
            CheckSmsVerifyCodeResponse resp = response.get();

            // 2. 判断核验结果，不对齐则抛出你原有的 401 状态码
            if (resp.getBody() == null || !"OK".equalsIgnoreCase(resp.getBody().getCode())) {
                throw new BusinessException(401, "验证码错误或已过期");
            }

            // 3. 【通过阿里云考核】开始操作本地数据库 MySQL
            // 拿着全小写的手机号字段去用户表查这个人
            User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                    .eq(User::getPhonenumber, phonenumber));

            // 4. 新用户自动开户（免注册登录）
            if (user == null) {
                user = new User();
                user.setPhonenumber(phonenumber); // 牢牢绑定手机号
                user.setUserName("user_" + phonenumber.substring(7)); // 随机生成用户名（后四位）
                user.setNickName("快捷用户_" + phonenumber.substring(7));

                // 由于是验证码快捷登录，可以设一个随机的不可逆乱码密码，防止别人猜解
                user.setPassword(passwordEncoder.encode(java.util.UUID.randomUUID().toString()));

                // 写入本地数据库，MyBatis-Plus 插入成功后会自动将生成的 userId 回填到 user 实体中
                userMapper.insert(user);
            }

            // 5. 将处理好的完整的 User 对象返回给 Controller 颁发 Token
            return user;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(500, "短信登录系统异常: " + e.getMessage());
        }
    }

    @Override
    public List<UserDTO> listUsers() {
        List<User> users = userMapper.selectList(null);
        return users.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    public UserDTO getById(Long id) {
        User u = userMapper.selectById(id);
        if (u == null) return null;
        return convertToDTO(u);
    }

    @Override
    public boolean saveUser(UserDTO dto) {
        User user = new User();
        user.setUserName(dto.getUserName());
        user.setNickName(dto.getNickName());
        user.setEmail(dto.getEmail());
        user.setPhonenumber(dto.getPhonenumber());
        return userMapper.insert(user) > 0;
    }

    private UserDTO convertToDTO(User u) {
        UserDTO dto = new UserDTO();
        dto.setUserId(u.getUserId());
        dto.setUserName(u.getUserName());
        dto.setNickName(u.getNickName());
        dto.setEmail(u.getEmail());
        dto.setPhonenumber(u.getPhonenumber());
        return dto;
    }
}
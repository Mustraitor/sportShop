package org.server.service.impl;
import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.server.common.exception.BusinessException;
import org.server.dto.UserDTO;
import org.server.entity.User;
import org.server.mapper.UserMapper;
import org.server.service.UserService;
import org.server.vo.UserVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

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
    @Override
    public UserVO getUserInfoById(Long userId) {
        // 1. 调用 MyBatis-Plus 自带的方法查询数据库
        User user = this.getById(userId);

        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 2. 将数据拷贝到 UserVO 中
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);


        return userVO;
    }

    @Override
    public UserVO updateUserInfo(Long userId, UserDTO.UpdateInfo updateInfo) {
        // 1. 构建要修改的 User 实体对象
        User user = new User();
        user.setUserId(userId);

        // 2. 将前端传过来的修改字段拷贝到 user 对象中
        BeanUtils.copyProperties(updateInfo, user);

        // 解决空串变 null 的防坑处理
        if (user.getAvatar() != null && user.getAvatar().trim().isEmpty()) {
            user.setAvatar(null);
        }

        // 3. 根据 ID 动态更新
        boolean success = this.updateById(user);
        if (!success) {
            // 💡 替换为你的 BusinessException，可以指定错误码（比如 500 或者自定义的 4002 等）
            throw new BusinessException(500, "修改用户信息失败");
        }

        // 4. 返回最新数据
        return this.getUserInfoById(userId);
    }
    @Override
    public void register(UserDTO.UserRegisterDTO registerDTO) {
        // 1. 检查用户名是否存在 (利用 MyBatis-Plus 的 selectOne)
        User existingUser = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User>()
                        .eq("user_name", registerDTO.getUsername()) // 这里的 "user_name" 是数据库列名
        );

        if (existingUser != null) {
            throw new BusinessException(400, "用户名已被注册");
        }

        // 2. 插入新用户
        User newUser = new User();
        newUser.setUserName(registerDTO.getUsername());
        newUser.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        String randomNick = "用户_" + UUID.randomUUID().toString().substring(0, 8);
        newUser.setNickName(randomNick);
        userMapper.insert(newUser);
    }

    @Override
    public User findByPhonenumber(String phonenumber) {
        return userMapper.findByPhonenumber(phonenumber);
    }

    @Override
    @Transactional
    public User findOrCreateByPhonenumber(String phonenumber) {
        String phone = phonenumber.trim();
        User user = userMapper.findByPhonenumber(phone);
        if (user == null) {
            System.out.println("=== 正在执行新用户创建逻辑 ===");
            user = new User();

            String suffix = phone.length() >= 4 ? phone.substring(phone.length() - 4) : phone;
            user.setUserName("手机用户_" + suffix);

            user.setNickName("用户");
            user.setPhonenumber(phone);
            user.setUserType("00");
            user.setSex("0");
            user.setStatus("0");
            user.setDelFlag("0");
            user.setCreateBy("system");
            user.setCreateTime(new Date());
            user.setUpdateBy("system");
            user.setUpdateTime(new Date());

            String randomPwd = UUID.randomUUID().toString().substring(0, 8);
            user.setPassword(passwordEncoder.encode(randomPwd));
            System.out.println("=== 调试: 此时的 phone = " + phone);
            System.out.println("=== 调试: 此时的 suffix = " + suffix);
            System.out.println("=== 调试: 准备插入的 userName = " + user.getUserName());
            userMapper.insert(user);
        }
        return user;
    }
    /**
     * 🎯 新增：实现获取用户余额逻辑
     */
    @Override
    public BigDecimal getUserBalance(Long userId) {
        User user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        // 如果数据库余额字段为 null，默认返回 0
        return user.getBalance() != null ? user.getBalance() : BigDecimal.ZERO;
    }
    /**
     * 🎯 新增：原子增加用户余额逻辑（防并发覆写隐患）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void increaseBalance(Long userId, BigDecimal amount) {
        // 1. 参数合法性校验
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(400, "充值金额必须大于 0");
        }

        // 2. 检查用户是否存在
        User user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        // 3. 核心并发安全更新：利用数据库底层实现 balance = balance + amount
        // 对应生成的 SQL 类似于：UPDATE user SET balance = balance + #{amount} WHERE user_id = #{userId}
        boolean success = this.update(new LambdaUpdateWrapper<User>()
                .eq(User::getUserId, userId)
                .setSql("balance = balance + " + amount.doubleValue())); // 将 BigDecimal 转换为符合你实体类的 double 值注入 SQL

        if (!success) {
            throw new BusinessException(500, "钱包账户充值失败，请稍后重试");
        }
    }
}














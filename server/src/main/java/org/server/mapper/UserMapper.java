package org.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.server.entity.User;

import java.math.BigDecimal;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT user_name FROM sys_user WHERE user_id = #{userId}")
    String selectUserNameById(@Param("userId") Long userId);
    /**
     * 原子扣减用户余额
     * 关键防御：WHERE balance >= #{amount} 确保钱不够时绝对扣不成功
     */
    @Update("UPDATE sys_user " +
            "SET balance = balance - #{amount} " +
            "WHERE user_id = #{userId} " +
            "AND balance >= #{amount}")
    int decreaseBalance(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    @Mapper
    @Select("SELECT * FROM sys_user WHERE phonenumber = #{phonenumber}")
    User findByPhonenumber(@Param("phonenumber") String phonenumber);

}

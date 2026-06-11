package org.server.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("sys_user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long userId;

    private String userName;

    private String nickName;

    private String password;

    private String email;

    private String phonenumber;

    private String avatar;

    private String sex;

    private BigDecimal balance;
}

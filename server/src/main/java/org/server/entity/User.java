package org.server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("sys_user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long userId;
    private String userName;
    private String nickName;
    private String userType;      // 对应 user_type
    private String email;
    private String phonenumber;   // 对应 phonenumber
    private String sex;
    private String avatar;
    private String password;
    private String status;        // 对应 status
    private String delFlag;       // 对应 del_flag
    private String loginIp;
    private Date loginDate;
    private String createBy;
    private Date createTime;
    private String updateBy;
    private Date updateTime;
    private String remark;
    private BigDecimal balance;
}

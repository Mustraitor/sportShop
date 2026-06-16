package org.server.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class UserVO {
    private Long userId;
    private String userName;
    private String nickName;
    private String email;
    private String phonenumber;
    private String avatar;
    private String sex;
    private BigDecimal balance;
}
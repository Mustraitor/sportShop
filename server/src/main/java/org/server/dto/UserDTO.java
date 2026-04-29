package org.server.dto;

import lombok.Data;

@Data
public class UserDTO {

    private Long userId;
    private String userName;
    private String nickName;
    private String email;
    private String phonenumber;
}

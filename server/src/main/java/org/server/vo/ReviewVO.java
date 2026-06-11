package org.server.vo;

import lombok.Data;

@Data
public class ReviewVO {
    private Long id;
    private String userName;   // 来自 sys_user.user_name
    private Integer rating;
    private String content;
    private String createdAt;  // 格式化成 yyyy-MM-dd HH:mm:ss
}
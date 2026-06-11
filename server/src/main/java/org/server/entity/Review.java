package org.server.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("review")
public class Review {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;      // 用户ID，对应 sys_user.user_id
    private Long productId;   // 商品ID
    private Integer rating;   // 评分 1~5
    private String content;   // 评价内容
    private LocalDateTime createdAt; // 创建时间，数据库自动生成
}
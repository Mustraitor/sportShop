package org.server.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("product_image") // 关联数据库表
public class ProductImage {

    @TableId(type = IdType.AUTO) // 主键自增
    private Long id;

    private Long productId;

    private String url;

    private Integer sort;

    private LocalDateTime createdAt;
}
package org.server.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Product {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long categoryId; // 对应数据库的 category_id

    private String name;

    private String description;

    private String mainImage; // 对应数据库的 main_image（驼峰命名）

    private BigDecimal price;

    private Integer status; // TINYINT 对应 Java 的 Integer

    @TableLogic
    private Integer isDeleted; // 对应数据库的 is_deleted，逻辑删除标识

    private Integer stock;

    private LocalDateTime createdAt; // 对应数据库的 created_at

    private LocalDateTime updatedAt; // 对应数据库的 updated_at
}
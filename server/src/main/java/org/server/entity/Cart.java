package org.server.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("cart") // 🌟 告诉 MyBatis-Plus 对应数据库的 cart 表
public class Cart {

    /**
     * 主键ID (自增)
     */
    @TableId(type = IdType.AUTO) // 🌟 完美对应 AUTO_INCREMENT 策略
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * SKU ID
     */
    private Long skuId;

    /**
     * 购买数量 (默认 1)
     */
    private Integer quantity;

    /**
     * 是否选中（1=选中，0=未选中，默认 1）
     * 💡 提示：数据库中是 TINYINT，在 Java 中企业级规范通常用 Integer 或 Boolean 接收
     */
    private Integer checked;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
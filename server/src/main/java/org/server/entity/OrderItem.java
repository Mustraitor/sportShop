package org.server.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("order_item")
public class OrderItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联的主订单ID */
    private Long orderId;

    /** 商品ID */
    private Long productId;

    /** SKU（规格）ID */
    private Long skuId;

    /** 商品名称快照（防止商品被后台修改名） */
    private String productName;

    /** 规格名称快照（例如：颜色:黑色;尺寸:42码） */
    private String skuName;

    /** 购买时的单价快照 */
    private BigDecimal price;

    /** 购买数量 */
    private Integer quantity;

    /** 该商品项的总价 (price * quantity) */
    private BigDecimal totalPrice;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
package org.server.vo;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CartItemVO {
    private Long id;              // 购物车记录ID（来自 cart 表）
    private Long productId;       // 商品ID（来自 cart 表）
    private String productName;   // 商品名称（来自 product 表）
    private String mainImage;     // 商品主图（来自 product 表）
    private Long skuId;           // SKU ID（来自 cart 表）
    private String skuName;       // SKU规格名称（来自 product_sku 表）
    private BigDecimal price;     // 商品单价（来自 product_sku 表）
    private Integer quantity;     // 购买数量（来自 cart 表）
    private Integer stock;        // 当前最新库存（来自 product_sku 表）
    private Integer checked;      // 是否选中（来自 cart 表）
}
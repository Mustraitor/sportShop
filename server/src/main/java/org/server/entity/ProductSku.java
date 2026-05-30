package org.server.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ProductSku {
    private Long id;
    private Long productId;
    private String skuName;
    private BigDecimal price;
    private Integer stock;
    private String skuCode;
    private LocalDateTime createdAt;
}
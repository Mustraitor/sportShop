package org.server.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("order_shipping")
public class OrderShipping {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private String courierCompany;
    private String trackingNo;
    private String status;       // 可以存 "SHIPPED", "DELIVERED" 等
    private LocalDateTime shippedAt;
    private LocalDateTime deliveredAt;
}
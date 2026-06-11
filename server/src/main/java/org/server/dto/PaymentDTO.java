package org.server.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaymentDTO {
    @NotNull(message = "订单ID不能为空")
    private Long orderId;

//    @NotNull(message = "支付方式不能为空")
    private Integer paymentMethod; // 1-模拟支付, 2-余额...
}
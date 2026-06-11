package org.server.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

public class OrderDTO {

    /**
     * 对应 POST /order 请求体
     */
    @Data
    public static class Create {

        @NotNull(message = "收货地址不能为空")
        private Long addressId;

        @NotEmpty(message = "请选择要结算的商品")
        private List<Long> cartIds;
    }
    @Data
    public static class PageReq {
        @NotNull(message = "页码不能为空")
        @Min(value = 1, message = "页码最小为1")
        private Integer page;

        @NotNull(message = "每页条数不能为空")
        @Min(value = 1, message = "每页条数最小为1")
        private Integer pageSize;

        /** 状态筛选：0-待支付，1-待收货，2-已完成... (不传代表查全部) */
        private Integer status;
    }
    @Data
    public static class ShipReq {
        @NotBlank(message = "快递单号不能为空")
        private String trackingNumber;

        @NotBlank(message = "快递公司不能为空")
        private String company;
    }
}
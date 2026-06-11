package org.server.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.server.entity.OrderItem;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class OrderVO {

    /**
     * 订单分页大对象 (data)
     */
    @Data
    public static class PageResult {
        private Long total;
        private List<OrderListItem> list;
    }

    /**
     * 列表中的单个订单对象
     */
    @Data
    public static class OrderListItem {
        private Long id;
        private Integer status;
        private BigDecimal totalAmount;

        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDateTime createdAt;

        // 该订单包含的紧凑版商品明细
        private List<ItemMin> items;
    }

    /**
     * 创建订单响应对象
     */
    @Data
    public static class Create {
        private Long orderId;
        private BigDecimal totalAmount;
        private BigDecimal payAmount;
    }

    /**
     * 订单详情响应对象
     */
    @Data
    public static class Detail {
        private Long id;
        private Long userId;
        private Long addressId;
        private BigDecimal totalAmount;
        private BigDecimal payAmount;
        private Integer status;
        private Integer paymentType;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime createdAt;

        // 该订单下的所有商品明细
        private List<OrderItem> items;
    }

    /**
     * 列表中紧凑版商品明细
     */
    @Data
    public static class ItemMin {
        private String productName;
        private String skuName;
        private Integer quantity;
    }

    /**
     * 取消订单返回结果
     */
    @Data
    public static class CancelResult {
        private Long orderId;
        private Integer status;           // 对应状态：2 (已取消)
        private String statusDescription;  // 状态描述："已取消"

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime updatedAt;
    }
    /**
     * 发货返回结果
     */
    @Data
    public static class ShipResult {
        private Long id;
        private Integer status;
        private String courierCompany;
        private String trackingNo;
    }

    /**
     * 确认收货返回结果
     */
    @Data
    public static class ReceiveResult {
        private Long id;
        private Integer status;
    }
}
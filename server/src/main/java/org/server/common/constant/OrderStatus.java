package org.server.common.constant;

public enum OrderStatus {
    PENDING_PAY(0, "待支付"),
    PENDING_RECEIVE(1, "待收货"),
    CANCELLED(2, "已取消"),
    COMPLETED(3, "已完成");

    private final Integer code;
    private final String description;

    OrderStatus(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    public Integer getCode() { return code; }
    public String getDescription() { return description; }
}
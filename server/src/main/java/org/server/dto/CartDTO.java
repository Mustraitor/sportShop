package org.server.dto;

import lombok.Data;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.Range;

/**
 * 购物车模块 所有请求参数 DTO 的统一大类
 */
public class CartDTO {

    /**
     * 新增/加入购物车所使用的内部 DTO 类
     */
    @Data
    public static class CartAddDTO {

        @NotNull(message = "商品ID不能为空")
        private Long productId;

        @NotNull(message = "SKU ID不能为空")
        private Long skuId;

        @NotNull(message = "购买数量不能为空")
        @Min(value = 1, message = "购买数量至少为 1 件")
        @Max(value = 99, message = "单品单次添加不能超过 99 件")
        private Integer quantity;
        // 游客 ID
//        @NotBlank(message = "游客ID不能为空")
        private String guestId;
    }

    /**
     * 步进更新购物车数量所使用的内部 DTO 类
     */
    @Data
    public static class CartUpdateStepDTO {

        @NotBlank(message = "操作类型不能为空")
        @Pattern(regexp = "^(add|sub)$", message = "操作类型不合法，只能传 add 或 sub")
        private String action;
        // 游客ID
//        @NotBlank(message = "游客ID不能为空")
        private String guestId;
    }
    @Data
    public static class CartCheckedDTO {
        @NotNull(message = "勾选状态不能为空")
        @Range(min = 0, max = 1, message = "勾选状态不合法，只能是 0 或 1")
        private Integer checked;
        // 游客ID
//        @NotBlank(message = "游客ID不能为空")
        private String guestId;
    }

}
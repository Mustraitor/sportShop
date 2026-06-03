package org.server.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * 地址模块 所有请求参数 DTO 的统一大类
 */
public class AddressDTO {

    /**
     * 新增地址所使用的内部 DTO 类
     */
    @Data
    public static class AddDTO {

        @NotBlank(message = "收货人姓名不能为空")
        private String name;

        // 🌟 这里就是防线！不仅不能为空，还必须符合中国大陆11位手机号的规则
        @NotBlank(message = "收货人电话不能为空")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        private String phone;

        @NotBlank(message = "省份不能为空")
        private String province;

        @NotBlank(message = "城市不能为空")
        private String city;

        @NotBlank(message = "区/县不能为空")
        private String district;

        @NotBlank(message = "详细地址不能为空")
        private String detail;

        @NotNull(message = "是否默认地址状态不能为空")
        private Integer isDefault;
    }
    @Data
    public static class UpdateDTO {
        @NotBlank(message = "收货人姓名不能为空")
        private String name;

        @NotBlank(message = "收货人电话不能为空")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        private String phone;

        @NotBlank(message = "省份不能为空")
        private String province;

        @NotBlank(message = "城市不能为空")
        private String city;

        @NotBlank(message = "区/县不能为空")
        private String district;

        @NotBlank(message = "详细地址不能为空")
        private String detail;

        @NotNull(message = "是否默认地址状态不能为空")
        private Integer isDefault;
    }
}
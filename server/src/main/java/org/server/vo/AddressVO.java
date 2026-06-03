package org.server.vo;

import lombok.Data;

/**
 * 地址模块 所有返回对象 VO 的统一大类
 */
public class AddressVO {

    /**
     * 地址基础信息返回的内部 VO 类
     */
    @Data
    public static class BaseVO {
        private Long id;
        private String name;
        private String phone;
        private String province;
        private String city;
        private String district;
        private String detail;
        private Integer isDefault;
    }

}
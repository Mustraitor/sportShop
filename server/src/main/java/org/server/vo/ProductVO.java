package org.server.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

/**
 * 商品模块所有返回给前端的数据对象（JSON 结构）总容器
 */
public class ProductVO {

    /**
     * 1. 商品分页列表展示的简要数据 (Simple)
     * 只保留最核心的、列表页拉取需要显示的字段
     */
    @Data
    public static class Simple {
        private Long id;
        private String name;
        private Long categoryId;
        private BigDecimal price;
        private String mainImage;
        private Integer status;
    }

    /**
     * 2. 商品详情展示的巨无霸嵌套数据 (Detail)
     * 面向 /product/{id} 接口，包含基本信息、轮播图、以及下面的 SKU 列表
     */
    @Data
    public static class Detail {
        private Long id;
        private String name;
        private Long categoryId;
        private String description;
        private BigDecimal price;
        private Integer stock;
        private Integer status;
        private String mainImage;

        // 商品多张轮播副图 URL 数组
        private List<String> images;

        // 嵌套下方定义的 Sku 内部类列表
        private List<Sku> skus;
    }

    /**
     * 3. 商品详情里嵌套展示的具体规格数据 (Sku)
     * 只在详情页用户选尺码/颜色时使用，不对外独立暴露，只被上面的 Detail 引用
     */
    @Data
    public static class Sku {
        private Long id;
        private String skuName;
        private BigDecimal price;
        private Integer stock;
    }
}
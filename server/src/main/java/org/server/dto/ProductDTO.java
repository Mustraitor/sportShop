package org.server.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

/**
 * 商品模块所有请求参数的统一容器
 */
public class ProductDTO {

    /**
     * 1. 商品分页列表查询请求参数 (把我们之前写的 Query 挪进来)
     */
    @Data
    public static class Query {
        private Integer page;
        private Integer size;
        private Long categoryId;
        private String keyword;
        private BigDecimal minPrice;
        private BigDecimal maxPrice;
        private String sort;
        private Integer status;
    }

    /**
     * 2. 创建商品请求的主包参数 (对应原来的 ProductCreateDTO)
     */
    @Data
    public static class Create {
        private String name;
        private Long categoryId;
        private String description;
        private String mainImage;
        private BigDecimal price;
        private Integer stock;
        private Integer status;

        // 嵌套图片列表
        private List<String> images;

        // 嵌套静态内部类 SKU 列表
        private List<SkuCreate> skus;
    }

    /**
     * 3. 创建商品时嵌套的 SKU 参数 (对应原来的 ProductSkuCreateDTO)
     * 注意：必须是 public static，否则外面无法作为实体类正常反序列化
     */
    @Data
    public static class SkuCreate {
        private String skuName;
        private BigDecimal price;
        private Integer stock;
    }
    /**
     * 4. 更新商品请求的主包参数 (结构与 Create 类似)
     */
    @Data
    public static class Update {
        private String name;
        private Long categoryId;
        private String description;
        private String mainImage;
        private BigDecimal price;
        private Integer stock;
        private Integer status;

        private List<String> images;
        private List<SkuCreate> skus; // 复用之前定义的 SkuCreate 内部类即可
    }
}
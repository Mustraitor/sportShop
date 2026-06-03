package org.server.dto;

import lombok.Data;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * 商品模块所有请求参数的统一容器
 */
public class ProductDTO {

    /**
     * 1. 商品分页列表查询请求参数
     */
    @Data
    public static class Query {
        @NotNull(message = "页码不能为空")
        @Min(value = 1, message = "页码最小为1")
        private Integer page;

        @NotNull(message = "每页条数不能为空")
        @Min(value = 1, message = "每页条数最小为1")
        @Max(value = 100, message = "每页条数最大为100")
        private Integer size;

        private Long categoryId;
        private String keyword;

        @PositiveOrZero(message = "最小价格不能为负数")
        private BigDecimal minPrice;

        @PositiveOrZero(message = "最大价格不能为负数")
        private BigDecimal maxPrice;

        private String sort;
        private Integer status;
    }

    /**
     * 2. 创建商品请求的主包参数
     */
    @Data
    public static class Create {
        @NotBlank(message = "商品名称不能为空")
        @Size(max = 100, message = "商品名称长度不能超过100个字符")
        private String name;

        @NotNull(message = "商品分类不能为空")
        private Long categoryId;

        private String description;

        @NotBlank(message = "商品主图不能为空")
        private String mainImage;

        @NotNull(message = "商品价格不能为空")
        @DecimalMin(value = "0.00", message = "商品价格不能为负数")
        private BigDecimal price;

        @NotNull(message = "商品总库存不能为空")
        @Min(value = 0, message = "商品库存不能为负数")
        private Integer stock;

        @NotNull(message = "商品状态不能为空")
        private Integer status; // 0=下架，1=上架

        // 嵌套图片列表：列表本身不能为空，且里面至少包含1张图
        @NotEmpty(message = "商品图片列表不能为空")
        private List<String> images;

        // 🌟 核心防御：必须加 @Valid 开启级联校验！否则内部 SkuCreate 的校验注解会直接失效！
        @NotEmpty(message = "商品SKU列表不能为空")
        @Valid
        private List<SkuCreate> skus;
    }

    /**
     * 3. 创建商品时嵌套的 SKU 参数
     */
    @Data
    public static class SkuCreate {
        @NotBlank(message = "SKU规格名称不能为空")
        private String skuName;

        @NotNull(message = "SKU价格不能为空")
        @DecimalMin(value = "0.00", message = "SKU价格不能为负数")
        private BigDecimal price;

        @NotNull(message = "SKU库存不能为空")
        @Min(value = 0, message = "SKU库存不能为负数")
        private Integer stock;
    }

    /**
     * 4. 更新商品请求的主包参数
     */
    @Data
    public static class Update {
        @NotBlank(message = "商品名称不能为空")
        private String name;

        @NotNull(message = "商品分类不能为空")
        private Long categoryId;

        private String description;

        @NotBlank(message = "商品主图不能为空")
        private String mainImage;

        @NotNull(message = "商品价格不能为空")
        @DecimalMin(value = "0.00", message = "商品价格不能为负数")
        private BigDecimal price;

        @NotNull(message = "商品总库存不能为空")
        @Min(value = 0, message = "商品库存不能为负数")
        private Integer stock;

        @NotNull(message = "商品状态不能为空")
        @Min(value = 0, message = "商品状态不合法，只能是0(下架)或1(上架)")
        @Max(value = 1, message = "商品状态不合法，只能是0(下架)或1(上架)")
        private Integer status = 1;

        @NotEmpty(message = "商品图片列表不能为空")
        private List<String> images;

        // 🌟 同样开启嵌套修改级联校验
        @NotEmpty(message = "商品SKU列表不能为空")
        @Valid
        private List<SkuUpdate> skus;
    }

    /**
     * 5. 更新商品时嵌套的 SKU 参数
     */
    @Data
    public static class SkuUpdate {
        @NotNull(message = "SKU ID 不能为空") // 修改时，原有的 SKU 必须带上主键 ID 才能定向更新
        private Long id;

        @NotBlank(message = "SKU规格名称不能为空")
        private String skuName;

        @NotNull(message = "SKU价格不能为空")
        @DecimalMin(value = "0.00", message = "SKU价格不能为负数")
        private BigDecimal price;

        @NotNull(message = "SKU库存不能为空")
        @Min(value = 0, message = "SKU库存不能为负数")
        private Integer stock;
    }
}
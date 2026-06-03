package org.server.controller;

import org.server.common.Result;
import org.server.dto.ProductDTO;
import org.server.service.ProductService;
import org.server.vo.PageResult;
import org.server.vo.ProductVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/product")
public class ProductController {

    @Autowired
    private ProductService productService;

    @GetMapping("/list")
    public Result<PageResult<ProductVO.Simple>> pageQuery(ProductDTO.Query queryDTO) {
        PageResult<ProductVO.Simple> pageResult = productService.pageQuery(queryDTO);
        return Result.success(pageResult);
    }
    /**
     * 获取商品详情 (包含轮播图、SKU列表)
     * 路由: GET /product/{id}
     * 请求参数通过路径变量传递 (例如: /product/1001)
     */
    @GetMapping("/{id}")
    public Result<ProductVO.Detail> getProductDetail(@PathVariable("id") Long id) {
        ProductVO.Detail detailVO = productService.getDetailById(id);
        if (detailVO == null) {
            // 对齐业务逻辑：找不到商品或被下架删除时返回友好提示
            return Result.error(404, "该商品不存在或已下架");
        }
        return Result.success(detailVO);
    }

    @PostMapping
    public Result<Void> createProduct(@RequestBody @Validated ProductDTO.Create createDTO) {
        productService.createProduct(createDTO);
        return Result.success("创建商品成功", null);
    }
    /**
     * 更新商品 (包含多张副图、多条SKU信息重组)
     * 路由: POST /product/{id}
     */
    @PostMapping("/{id}")
    public Result<Void> updateProduct(@PathVariable("id") Long id, @RequestBody @Validated ProductDTO.Update updateDTO) {
        // 校验目标商品是否存在
        ProductVO.Detail currentProduct = productService.getDetailById(id);
        if (currentProduct == null) {
            return Result.error(404, "要修改的商品不存在或已被彻底删除");
        }

        productService.updateProduct(id, updateDTO);
        return Result.success("商品已更新", null); // 完全对齐你要求的返回格式
    }
    /**
     * 逻辑删除商品 (将 is_deleted 改为 1)
     * 路由: DELETE /product/{id}
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteProduct(@PathVariable("id") Long id) {
        // 前置校验：看商品是否存在
        ProductVO.Detail currentProduct = productService.getDetailById(id);
        if (currentProduct == null) {
            return Result.error(404, "该商品不存在或已被删除");
        }

        productService.deleteProductById(id);
        return Result.success("deleted", null); // 严格对齐你要求的 msg: "deleted"
    }

}
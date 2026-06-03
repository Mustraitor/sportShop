package org.server.controller;

import org.server.common.Result;
import org.server.dto.CategoryDTO;
import org.server.service.CategoryService;
import org.server.vo.CategoryVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/category")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    /**
     * 查询分类树形列表
     * 路由: GET /category/list
     */
    @GetMapping("/list")
    public Result<List<CategoryVO>> getCategoryList() {
        List<CategoryVO> treeData = categoryService.getCategoryTree();
        // 严格对齐你要求的成功返回格式：code: 200, msg: "success", data: [...]
        return Result.success(treeData);
    }

    @PostMapping
    public Result<CategoryVO.Simple> createCategory(@RequestBody CategoryDTO.Create createDTO) {
        CategoryVO.Simple createdCategory = categoryService.createCategory(createDTO);

        // 严格对齐要求的格式：msg 为 "分类创建成功"，data 为刚创建完含新 id 的对象
        return Result.success("分类创建成功", createdCategory);
    }
    /**
     * 修改分类
     * 路由: PUT /category
     */
    @PutMapping
    public Result<?> updateCategory(@RequestBody CategoryDTO.Update updateDTO) {
        // 前置基础校验
        if (updateDTO.getId() == null) {
            return Result.error(400, "分类ID（id）不能为空");
        }

        try {
            CategoryVO.Simple result = categoryService.updateCategory(updateDTO);
            // 严格对齐你要求的成功格式
            return Result.success("success", result);
        } catch (IllegalArgumentException | IllegalStateException e) {
            // 敏锐捕捉到“循环依赖”或“自己设为自己父级”的报错，优雅拦截并提示前端
            return Result.error(400, e.getMessage());
        }
    }
    /**
     * 安全物理删除分类
     * 路由: DELETE /category/{id}
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteCategory(@PathVariable("id") Long id) {
        try {
            categoryService.deleteCategoryById(id);
            // 严格对齐成功格式：msg 为 "分类删除成功"，data 为 null
            return Result.success("分类删除成功", null);
        } catch (IllegalArgumentException e) {
            // 敏锐捕获业务异常，并根据不同的异常文本对齐你要求的 400 错误提示
            String errorMsg = e.getMessage();

            if ("存在子分类，无法删除".equals(errorMsg)) {
                return Result.error(400, "存在子分类，无法删除");
            }
            if ("该分类下存在商品，禁止删除".equals(errorMsg)) {
                return Result.error(400, "该分类下存在商品，禁止删除");
            }

            // 处理其他常规异常（例如分类本来就找不到）
            return Result.error(400, errorMsg);
        }
    }

}
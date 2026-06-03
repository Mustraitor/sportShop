package org.server.service;

import org.server.vo.CategoryVO;
import org.server.dto.CategoryDTO;
import java.util.List;

/**
 * 商品分类业务层接口
 * 保持契约的纯净，所有导包均在头部统一管理
 */
public interface CategoryService {

    /**
     * 获取全部分类树形结构
     */
    List<CategoryVO> getCategoryTree();

    /**
     * 创建新分类，并返回创建后的简要信息
     */
    CategoryVO.Simple createCategory(CategoryDTO.Create createDTO);

    /**
     * 安全修改分类，包含死循环依赖校验
     */
    CategoryVO.Simple updateCategory(CategoryDTO.Update updateDTO);

    /**
     * 安全的物理删除分类（拦截一切破坏性隐患）
     */
    void deleteCategoryById(Long id);
}
package org.server.service;

import org.server.dto.ProductDTO;
import org.server.vo.PageResult;
import org.server.vo.ProductVO;

import java.util.List;

public interface ProductService {

    /**
     * 商品条件分页查询
     * @param queryDTO 包含页码、每页数量、分类、关键词、价格区间等筛选条件
     * @return 包含总条数、总页数和当前页真实商品列表的分页结果对象
     */
    PageResult<ProductVO.Simple> pageQuery(ProductDTO.Query queryDTO);

    ProductVO.Detail getDetailById(Long id);

    void createProduct(ProductDTO.Create createDTO);

    void updateProduct(Long id, ProductDTO.Update updateDTO);

    void deleteProductById(Long id);

    List<ProductVO.Simple> searchByKeyword(String keyword);
}
package org.server.service.impl;

import org.server.dto.ProductDTO;
import org.server.entity.Product;
import org.server.entity.ProductImage;
import org.server.entity.ProductSku;
import org.server.mapper.ProductImageMapper;
import org.server.mapper.ProductMapper;
import org.server.mapper.ProductSkuMapper;
import org.server.service.ProductService;
import org.server.vo.PageResult;
import org.server.vo.ProductVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private ProductImageMapper productImageMapper;

    @Autowired
    private ProductSkuMapper productSkuMapper;

    @Override
    public PageResult<ProductVO.Simple> pageQuery(ProductDTO.Query queryDTO) {
        int pageNum = (queryDTO.getPage() == null || queryDTO.getPage() < 1) ? 1 : queryDTO.getPage();
        int size = (queryDTO.getSize() == null || queryDTO.getSize() < 1) ? 10 : queryDTO.getSize();

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Product> mpPage =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageNum, size);

        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Product> queryWrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();

        queryWrapper
                .eq(queryDTO.getCategoryId() != null, Product::getCategoryId, queryDTO.getCategoryId())
                .eq(queryDTO.getStatus() != null, Product::getStatus, queryDTO.getStatus())
                .like(org.springframework.util.StringUtils.hasText(queryDTO.getKeyword()), Product::getName, queryDTO.getKeyword())
                .ge(queryDTO.getMinPrice() != null, Product::getPrice, queryDTO.getMinPrice())
                .le(queryDTO.getMaxPrice() != null, Product::getPrice, queryDTO.getMaxPrice())
                .eq(Product::getDelFlag, 0);

        if ("price_asc".equals(queryDTO.getSort())) {
            queryWrapper.orderByAsc(Product::getPrice);
        } else if ("price_desc".equals(queryDTO.getSort())) {
            queryWrapper.orderByDesc(Product::getPrice);
        } else {
            queryWrapper.orderByDesc(Product::getId);
        }

        productMapper.selectPage(mpPage, queryWrapper);

        List<Product> products = mpPage.getRecords();
        List<ProductVO.Simple> voList = products.stream().map(product -> {
            ProductVO.Simple vo = new ProductVO.Simple();
            org.springframework.beans.BeanUtils.copyProperties(product, vo);
            return vo;
        }).collect(Collectors.toList());

        return new PageResult<>(mpPage.getTotal(), pageNum, size, voList);
    }

    @Override
    public ProductVO.Detail getDetailById(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null || product.getDelFlag() == 1) {
            return null;
        }

        ProductVO.Detail detailVO = new ProductVO.Detail();
        org.springframework.beans.BeanUtils.copyProperties(product, detailVO);

        List<ProductImage> images = productImageMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ProductImage>()
                        .eq(ProductImage::getProductId, id)
                        .orderByAsc(ProductImage::getSort)
        );
        List<String> imageUrls = images.stream().map(ProductImage::getUrl).collect(Collectors.toList());
        detailVO.setImages(imageUrls);

        List<ProductSku> skus = productSkuMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ProductSku>()
                        .eq(ProductSku::getProductId, id)
        );
        List<ProductVO.Sku> skuVOs = skus.stream().map(sku -> {
            ProductVO.Sku vo = new ProductVO.Sku();
            org.springframework.beans.BeanUtils.copyProperties(sku, vo);
            return vo;
        }).collect(Collectors.toList());

        detailVO.setSkus(skuVOs);
        return detailVO;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public void createProduct(ProductDTO.Create createDTO) {
        Product product = new Product();
        org.springframework.beans.BeanUtils.copyProperties(createDTO, product);
        product.setDelFlag(0);
        productMapper.insert(product);

        Long productId = product.getId();

        List<String> images = createDTO.getImages();
        if (images != null && !images.isEmpty()) {
            int sort = 0;
            for (String url : images) {
                ProductImage productImage = new ProductImage();
                productImage.setProductId(productId);
                productImage.setUrl(url);
                productImage.setSort(sort++);
                productImageMapper.insert(productImage);
            }
        }

        List<ProductDTO.SkuCreate> skus = createDTO.getSkus();
        if (skus != null && !skus.isEmpty()) {
            for (ProductDTO.SkuCreate skuDTO : skus) {
                ProductSku productSku = new ProductSku();
                org.springframework.beans.BeanUtils.copyProperties(skuDTO, productSku);
                productSku.setProductId(productId);
                productSku.setSkuCode("SKU_" + productId + "_" + System.currentTimeMillis() + (int)(Math.random()*900+100));
                productSkuMapper.insert(productSku);
            }
        }
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class) // 强事务保证
    public void updateProduct(Long id, ProductDTO.Update updateDTO) {
        // 1. 更新商品主表
        Product product = new Product();
        org.springframework.beans.BeanUtils.copyProperties(updateDTO, product);
        product.setId(id); // 锁死要更新的 19 位商品 ID
        productMapper.updateById(product);

        // 2. 清空并重建商品副图
        // 2.1 物理删除老图片
        productImageMapper.delete(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ProductImage>()
                        .eq(ProductImage::getProductId, id)
        );
        // 2.2 批量插入新图片
        List<String> images = updateDTO.getImages();
        if (images != null && !images.isEmpty()) {
            int sort = 0;
            for (String url : images) {
                ProductImage productImage = new ProductImage();
                productImage.setProductId(id);
                productImage.setUrl(url);
                productImage.setSort(sort++);
                productImageMapper.insert(productImage);
            }
        }

        // 3. 清空并重建商品 SKU
        // 3.1 物理删除老 SKU
        productSkuMapper.delete(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ProductSku>()
                        .eq(ProductSku::getProductId, id)
        );
        // 3.2 批量插入新 SKU
        List<ProductDTO.SkuCreate> skus = updateDTO.getSkus();
        if (skus != null && !skus.isEmpty()) {
            for (ProductDTO.SkuCreate skuDTO : skus) {
                ProductSku productSku = new ProductSku();
                org.springframework.beans.BeanUtils.copyProperties(skuDTO, productSku);
                productSku.setProductId(id);
                productSku.setSkuCode("SKU_" + id + "_" + System.currentTimeMillis() + (int)(Math.random()*900+100));
                productSkuMapper.insert(productSku);
            }
        }
    }

    // 在 ProductServiceImpl 类中追加实现：

    @Override
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class) // 涉及状态修改，建议带上事务
    public void deleteProductById(Long id) {
        // 使用 LambdaUpdateWrapper 快速执行：UPDATE product SET del_flag = 1 WHERE id = {id}
        productMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Product>()
                        .eq(Product::getId, id)
                        .set(Product::getDelFlag, 1)
        );
    }
}
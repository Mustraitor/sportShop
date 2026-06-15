package org.server.service.impl;

// 1. 框架核心及切面事务注解
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.server.common.exception.BusinessException;
import org.server.entity.Category;
import org.server.mapper.CategoryMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

// 2. MyBatis-Plus 核心及插件依赖
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

// 3. 项目内部实体、传输对象、持久层与视图层组件
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

// 4. Java 核心工具包
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private ProductImageMapper productImageMapper;

    @Autowired
    private ProductSkuMapper productSkuMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    // 统一注入 OSS 配置属性
    @Value("${aliyun.oss.endpoint}")
    private String endpoint;

    @Value("${aliyun.oss.bucket-name}")
    private String bucketName;

    /**
     * 辅助私有方法：构建基础 OSS 前缀，完美拼接：https://bucket.endpoint/upload/商品ID/
     */
    private String getOssBaseUrl(Long productId) {
        return "https://" + bucketName + "." + endpoint  + "/upload/";
    }

    @Override
    public PageResult<ProductVO.Simple> pageQuery(ProductDTO.Query queryDTO) {
        int pageNum = (queryDTO.getPage() == null || queryDTO.getPage() < 1) ? 1 : queryDTO.getPage();
        int size = (queryDTO.getSize() == null || queryDTO.getSize() < 1) ? 10 : queryDTO.getSize();

        Page<Product> mpPage = new Page<>(pageNum, size);
        LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper
                .eq(queryDTO.getCategoryId() != null, Product::getCategoryId, queryDTO.getCategoryId())
                .eq(queryDTO.getStatus() != null, Product::getStatus, queryDTO.getStatus())
                .like(StringUtils.hasText(queryDTO.getKeyword()), Product::getName, queryDTO.getKeyword())
                .ge(queryDTO.getMinPrice() != null, Product::getPrice, queryDTO.getMinPrice())
                .le(queryDTO.getMaxPrice() != null, Product::getPrice, queryDTO.getMaxPrice());

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
            BeanUtils.copyProperties(product, vo);

            // 主图出库时：动态拼接出完整直链，喂给前端商品列表
            if (StringUtils.hasText(product.getMainImage())) {
                vo.setMainImage(getOssBaseUrl(product.getId()) + product.getMainImage());
            }
            return vo;
        }).collect(Collectors.toList());

        return new PageResult<>(mpPage.getTotal(), pageNum, size, voList);
    }

    @Override
    public ProductVO.Detail getDetailById(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            return null;
        }

        ProductVO.Detail detailVO = new ProductVO.Detail();
        BeanUtils.copyProperties(product, detailVO);

        // 详情主图拼接
        if (StringUtils.hasText(product.getMainImage())) {
            detailVO.setMainImage(getOssBaseUrl(id) + product.getMainImage());
        }

        List<ProductImage> images = productImageMapper.selectList(
                new LambdaQueryWrapper<ProductImage>()
                        .eq(ProductImage::getProductId, id)
                        .orderByAsc(ProductImage::getSort)
        );

        // 详情副图列表出库时：循环拼装，将纯文件名转化为完整直链集合
        List<String> imageUrls = images.stream()
                .map(img -> getOssBaseUrl(id) + img.getUrl())
                .collect(Collectors.toList());
        detailVO.setImages(imageUrls);

        List<ProductSku> skus = productSkuMapper.selectList(
                new LambdaQueryWrapper<ProductSku>()
                        .eq(ProductSku::getProductId, id)
        );
        List<ProductVO.Sku> skuVOs = skus.stream().map(sku -> {
            ProductVO.Sku vo = new ProductVO.Sku();
            BeanUtils.copyProperties(sku, vo);
            return vo;
        }).collect(Collectors.toList());

        detailVO.setSkus(skuVOs);
        return detailVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createProduct(ProductDTO.Create createDTO) {
        // 🌟 核心防线：创建商品前，必须确保分类真实存在且没被删除
        if (createDTO.getCategoryId() != null) {
            Category category = categoryMapper.selectById(createDTO.getCategoryId());
            if (category == null) {
                // 抛出你量身定制的 BusinessException，指定 400 状态码
                throw new BusinessException(400, "创建失败：所选的商品分类不存在或已被删除！");
            }
        }

        // 1. 插入商品主表
        Product product = new Product();
        BeanUtils.copyProperties(createDTO, product);
        productMapper.insert(product);

        Long productId = product.getId();

        // 2. 插入商品副图
        List<String> images = createDTO.getImages();
        if (images != null && !images.isEmpty()) {
            int sort = 0;
            for (String url : images) {
                // 如果这张图已经是主图了，副图列表里就跳过它，防止重复展示
                if (StringUtils.hasText(product.getMainImage()) && url.equals(product.getMainImage())) {
                    continue;
                }
                ProductImage productImage = new ProductImage();
                productImage.setProductId(productId);
                productImage.setUrl(url);
                productImage.setSort(sort++); // 🌟 补上副图顺序，防图片乱序
                productImageMapper.insert(productImage);
            }
        }

        // 3. 插入商品 SKU 列表
        List<ProductDTO.SkuCreate> skus = createDTO.getSkus();
        if (skus != null && !skus.isEmpty()) {
            for (ProductDTO.SkuCreate skuDTO : skus) {
                ProductSku productSku = new ProductSku();
                BeanUtils.copyProperties(skuDTO, productSku);
                productSku.setProductId(productId);
                // 生成企业级高并发不重复的唯一标识 SKU 编码
                productSku.setSkuCode("SKU_" + productId + "_" + System.currentTimeMillis() + (int)(Math.random()*900+100));
                productSkuMapper.insert(productSku);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProduct(Long id, ProductDTO.Update updateDTO) {
        if (updateDTO.getCategoryId() != null) {
            Category category = categoryMapper.selectById(updateDTO.getCategoryId());
            if (category == null) {
                // 抛出运行时异常，触发事务自动回滚，并被 GlobalExceptionHandler 捕获
                throw new BusinessException(400, "修改失败：所选的商品分类不存在或已被删除！");
            }
        }
        // 1. 更新商品主表
        Product product = new Product();
        BeanUtils.copyProperties(updateDTO, product);
        product.setId(id);
        productMapper.updateById(product);

        // 2. 清空并重建商品副图
        productImageMapper.delete(
                new LambdaQueryWrapper<ProductImage>().eq(ProductImage::getProductId, id)
        );
        List<String> images = updateDTO.getImages();
        if (images != null && !images.isEmpty()) {
            int sort = 0;
            for (String url : images) {
                if (StringUtils.hasText(updateDTO.getMainImage()) && url.equals(updateDTO.getMainImage())) {
                    continue;
                }
                ProductImage productImage = new ProductImage();
                productImage.setProductId(id);
                productImage.setUrl(url);
                productImage.setSort(sort++);
                productImageMapper.insert(productImage);
            }
        }

        // 3. 智能双向差集更新 SKU
        List<ProductSku> dbOldSkus = productSkuMapper.selectList(
                new LambdaQueryWrapper<ProductSku>().eq(ProductSku::getProductId, id)
        );

        List<ProductDTO.SkuUpdate> incomingSkus = updateDTO.getSkus();

        if (incomingSkus != null && !incomingSkus.isEmpty()) {

            List<Long> incomingIds = incomingSkus.stream()
                    .map(ProductDTO.SkuUpdate::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            List<ProductSku> skusToDelete = dbOldSkus.stream()
                    .filter(oldSku -> !incomingIds.contains(oldSku.getId()))
                    .collect(Collectors.toList());

            for (ProductSku sku : skusToDelete) {
                productSkuMapper.deleteById(sku.getId());
            }

            for (ProductDTO.SkuUpdate skuDTO : incomingSkus) {
                if (skuDTO.getId() != null) {
                    ProductSku updateSku = new ProductSku();
                    BeanUtils.copyProperties(skuDTO, updateSku);
                    productSkuMapper.updateById(updateSku);
                } else {
                    ProductSku newSku = new ProductSku();
                    BeanUtils.copyProperties(skuDTO, newSku);
                    newSku.setProductId(id);
                    newSku.setSkuCode("SKU_" + id + "_" + System.currentTimeMillis() + (int)(Math.random() * 900 + 100));
                    productSkuMapper.insert(newSku);
                }
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProductById(Long id) {
        productMapper.deleteById(id);
    }
    @Override
    public List<ProductVO.Simple> searchByKeyword(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return Collections.emptyList();
        }

        // 1. 查询数据库
        QueryWrapper<Product> queryWrapper = new QueryWrapper<>();
        queryWrapper.like("name", keyword)
                .or()
                .like("description", keyword)
                .eq("status", 1);

        List<Product> productList = productMapper.selectList(queryWrapper);

        // 2. 核心转换逻辑：实例化 ProductVO.Simple
        return productList.stream().map(product -> {
            // 关键改动在这里：实例化内部类 Simple
            ProductVO.Simple vo = new ProductVO.Simple();

            // 记得赋值，BeanUtils 会自动匹配同名字段
            BeanUtils.copyProperties(product, vo);

            return vo;
        }).collect(Collectors.toList());
    }
}
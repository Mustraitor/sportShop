package org.server.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import org.server.entity.Product;
import org.server.entity.ProductImage;
import org.server.mapper.ProductImageMapper;
import org.server.mapper.ProductMapper;
import org.server.service.ProductImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class ProductImageServiceImpl extends ServiceImpl<ProductImageMapper, ProductImage> implements ProductImageService {

    @Value("${aliyun.oss.endpoint}")
    private String endpoint;

    @Value("${aliyun.oss.bucket-name}")
    private String bucketName;

    @Value("${aliyun.oss.access-key-id}")
    private String accessKeyId;

    @Value("${aliyun.oss.access-key-secret}")
    private String accessKeySecret;

    @Autowired
    private ProductMapper productMapper;

    private static final Pattern STRICT_IMAGE_PATTERN =
            Pattern.compile("^\\d+_(main|\\d+)\\.(png|jpg|jpeg|gif|webp)$", Pattern.CASE_INSENSITIVE);

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<String> uploadAndSyncImages(Long productId, String localPath, Integer sort) {
        if (localPath != null) {
            localPath = localPath.replace("\"", "").replace("'", "");
        }

        File targetFile = new File(localPath);
        if (!targetFile.exists()) {
            throw new RuntimeException("本地找不到该路径！" + targetFile.getAbsolutePath());
        }

        List<String> uploadedUrls = new ArrayList<>();
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

        try {
            if (targetFile.isDirectory()) {
                File[] files = targetFile.listFiles();
                if (files == null || files.length == 0) {
                    return uploadedUrls;
                }

                for (File file : files) {
                    if (file.isFile()) {
                        String fileName = file.getName();
                        if (!STRICT_IMAGE_PATTERN.matcher(fileName).matches()) {
                            System.out.println("【命名非法拦截】文件 [" + fileName + "] 不符合规范，已被安全忽略。");
                            continue;
                        }

                        Long finalProductId = parseProductIdFromFilename(fileName, productId);
                        Integer finalSort = fileName.toLowerCase().contains("main") ? 0 : sort;

                        String finalOssUrl = uploadSingleFileToOssAndDb(ossClient, file, finalProductId, finalSort);
                        uploadedUrls.add(finalOssUrl);
                    }
                }
            } else {
                if (!STRICT_IMAGE_PATTERN.matcher(targetFile.getName()).matches()) {
                    throw new RuntimeException("独立上传失败：单张图片的文件名 [" + targetFile.getName() + "] 不符合规范！");
                }
                String finalOssUrl = uploadSingleFileToOssAndDb(ossClient, targetFile, productId, sort);
                uploadedUrls.add(finalOssUrl);
            }
            return uploadedUrls;
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

    private String uploadSingleFileToOssAndDb(OSS ossClient, File file, Long productId, Integer sort) {
        String originalName = file.getName();
        String ossFileName = "upload/" + productId + "/" + originalName;
        String finalOssUrl = "https://" + bucketName + "." + endpoint + "/" + ossFileName;

        // 1. 推送文件覆盖OSS云端资源
        ossClient.putObject(bucketName, ossFileName, file);
        System.out.println("【OSS 云端覆盖】成功: " + ossFileName);

        // 2. 幂等维护副图表数据
        ProductImage existingImage = this.lambdaQuery()
                .eq(ProductImage::getProductId, productId)
                .eq(ProductImage::getUrl, finalOssUrl)
                .one();

        if (existingImage != null) {
            existingImage.setSort(sort);
            existingImage.setCreatedAt(LocalDateTime.now());
            this.updateById(existingImage);
            System.out.println("【副图表同步】更新成功");
        } else {
            ProductImage productImage = new ProductImage();
            productImage.setProductId(productId);
            productImage.setUrl(finalOssUrl);
            productImage.setSort(sort);
            productImage.setCreatedAt(LocalDateTime.now());
            this.save(productImage);
            System.out.println("【副图表同步】新增成功");
        }

        // 3. 联动更新商品主表主图链接
        if (sort == 0) {
            LambdaUpdateWrapper<Product> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(Product::getId, productId).set(Product::getMainImage, finalOssUrl);

            boolean updateProductMainImageSuccess = SqlHelper.retBool(productMapper.update(null, updateWrapper));

            if (updateProductMainImageSuccess) {
                System.out.println("【主表联动】更新商品主图链接成功");
            } else {
                System.out.println("【主表联动】未找到对应商品ID，主图链接未更新");
            }
        }

        return finalOssUrl;
    }

    private Long parseProductIdFromFilename(String fileName, Long defaultId) {
        return Long.parseLong(fileName.split("_")[0]);
    }
}
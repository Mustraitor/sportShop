package org.server.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.server.entity.ProductImage;
import org.server.mapper.ProductImageMapper;
import org.server.service.ProductImageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.time.LocalDateTime;
import java.util.UUID;

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

    @Override
    @Transactional(rollbackFor = Exception.class) // 涉及数据库写入，添加事务控制
    public String uploadAndSyncImages(Long productId, String localPath, Integer sort) {

        if (localPath != null) {
            localPath = localPath.replace("\"", "").replace("'", "");
        }

        File targetFile = new File(localPath);
        if (!targetFile.exists()) {
            return "错误：本地找不到该路径！" + targetFile.getAbsolutePath();
        }

        // 2. 初始化 OSS 客户端
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        int successCount = 0;

        try {
            // 3. 业务路由：判断是文件夹还是单张图片
            if (targetFile.isDirectory()) {
                File[] files = targetFile.listFiles();
                if (files == null || files.length == 0) {
                    return "提示：该文件夹是空的，没有可上传的图片。";
                }

                for (File file : files) {
                    if (file.isFile() && isImage(file.getName())) {
                        // 智能识别：优先尝试从文件名提取 productId (例如 "1001_main.jpg")
                        Long finalProductId = parseProductIdFromFilename(file.getName(), productId);
                        // 智能识别：如果名字带有 main 则是主图值 0，否则给 1
                        Integer finalSort = file.getName().toLowerCase().contains("main") ? 0 : sort;

                        uploadSingleFileToOssAndDb(ossClient, file, finalProductId, finalSort);
                        successCount++;
                    }
                }
                return "批量上传成功！一共成功同步了 " + successCount + " 张图片。";

            } else {
                // 单图上传
                String finalOssUrl = uploadSingleFileToOssAndDb(ossClient, targetFile, productId, sort);
                return "单图上传成功！链接为：" + finalOssUrl;
            }

        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

    /**
     * 辅助方法：上传单张图片并使用 MyBatis-Plus 的 save() 落地入库
     */
    private String uploadSingleFileToOssAndDb(OSS ossClient, File file, Long productId, Integer sort) {
        String originalName = file.getName();
        String ossFileName = "upload/" + UUID.randomUUID().toString().replace("-", "") + "_" + originalName;

        // 1. 推送至阿里云
        ossClient.putObject(bucketName, ossFileName, file);
        String finalOssUrl = "https://" + bucketName + "." + endpoint + "/" + ossFileName;

        // 2. 构造 MP 实体对象并落库
        ProductImage productImage = new ProductImage();
        productImage.setProductId(productId);
        productImage.setUrl(finalOssUrl);
        productImage.setSort(sort);
        productImage.setCreatedAt(LocalDateTime.now());

        // 3. 调用 ServiceImpl 自带的 save 方法（等价于通用 Mapper 的 insert）
        this.save(productImage);

        return finalOssUrl;
    }

    private boolean isImage(String fileName) {
        String name = fileName.toLowerCase();
        return name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".gif") || name.endsWith(".webp");
    }

    private Long parseProductIdFromFilename(String fileName, Long defaultId) {
        if (fileName.contains("_")) {
            try {
                return Long.parseLong(fileName.split("_")[0]);
            } catch (NumberFormatException ignored) {}
        }
        return defaultId;
    }
}
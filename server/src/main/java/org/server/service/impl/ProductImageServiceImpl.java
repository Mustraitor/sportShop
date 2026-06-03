package org.server.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.server.entity.ProductImage;
import org.server.mapper.ProductImageMapper;
import org.server.service.ProductImageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
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

    // 依然保持严格的文件名拦截，确保进 OSS 的数据名称是干净、规范的
    private static final Pattern STRICT_IMAGE_PATTERN =
            Pattern.compile("^\\d+_(main|\\d+)\\.(png|jpg|jpeg|gif|webp)$", Pattern.CASE_INSENSITIVE);

    @Override
    public List<String> uploadAndSyncImages(Long productId, String localPath, Integer sort) {
        if (localPath != null) {
            localPath = localPath.replace("\"", "").replace("'", "");
        }

        File targetFile = new File(localPath);
        if (!targetFile.exists()) {
            throw new RuntimeException("本地找不到该路径！" + targetFile.getAbsolutePath());
        }

        List<String> uploadedUrls = new ArrayList<>();
        // 初始化阿里云 OSS 客户端
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
                        // 1. 安全过滤
                        if (!STRICT_IMAGE_PATTERN.matcher(fileName).matches()) {
                            System.out.println("【命名非法拦截】文件 [" + fileName + "] 不符合规范，已被忽略。");
                            continue;
                        }

                        // 2. 纯粹的文件上云传输
                        String finalOssUrl = uploadSingleFileToOssOnly(ossClient, file, productId);
                        uploadedUrls.add(finalOssUrl);
                    }
                }
            } else {
                if (!STRICT_IMAGE_PATTERN.matcher(targetFile.getName()).matches()) {
                    throw new RuntimeException("独立上传失败：单张图片的文件名 [" + targetFile.getName() + "] 不符合规范！");
                }
                String finalOssUrl = uploadSingleFileToOssOnly(ossClient, targetFile, productId);
                uploadedUrls.add(finalOssUrl);
            }
            return uploadedUrls;
        } finally {
            // 确保无论如何都会关闭 OSS 客户端释放连接
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

    /**
     * 👑 纯净的 OSS 资源上传方法：无数据库依赖、零脏数据风险
     */
    private String uploadSingleFileToOssOnly(OSS ossClient, File file, Long productId) {
        // 1. 拿到纯粹的文件名（如: 10001_main.png）
        String originalName = file.getName();

        // 2. 云端 OSS 保持清晰的 upload/商品ID/ 目录划分（如果你的初始图片名自带ID，直接使用传入的 productId 即可）
        String ossFileName = "upload/" + originalName;

        // 3. 拼接出返回给前端或 Postman 格式的完整直链（完美对应你的第三张截图期望值）
        String finalOssUrl = "https://" + bucketName + "." + endpoint + "/" + ossFileName;

        // 4. 将纯文件流推送到阿里云 OSS
        ossClient.putObject(bucketName, ossFileName, file);
        System.out.println("【纯文件上云】推送成功: " + ossFileName);

        // 5. 👑 彻底移除所有数据库逻辑（不调 mapper，不 save，不 update）

        return finalOssUrl;
    }
}
package org.server.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.server.entity.ProductImage;

public interface ProductImageService extends IService<ProductImage> {

    /**
     * 核心业务：批量或单图解析本地路径并上传 OSS、保存至数据库
     * @param productId 商品ID
     * @param localPath 本地文件或文件夹路径
     * @param sort 初始排序值
     * @return 统一的操作结果提示
     */
    java.util.List<String> uploadAndSyncImages(Long productId, String localPath, Integer sort);
}
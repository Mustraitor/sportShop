package org.server.controller;

import org.server.common.Result; // 引入你规范的 Result 包装类
import org.server.service.ProductImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/oss")
public class ProductImageController {

    @Autowired
    private ProductImageService productImageService;

    /**
     * 规范整改后的统一入口 (使用 POST 接收 JSON 体，完美契合 Result<List<String>> 规范)
     * POST http://localhost:8080/oss/upload-product
     */
    @PostMapping("/upload-product")
    public Result<List<String>> uploadProductImage(@RequestBody Map<String, Object> params) {

        // 1. 安全解析参数，如果 productId 没传或者是 0，拿到 null 方便 Service 执行盲刷
        Long productId = (params.get("productId") != null && !params.get("productId").toString().equals("0"))
                ? Long.valueOf(params.get("productId").toString())
                : null;

        String localPath = (String) params.get("localPath");

        Integer sort = params.get("sort") != null ? Integer.valueOf(params.get("sort").toString()) : 1;

        // 2. 基础校验
        if (localPath == null || localPath.trim().isEmpty()) {
            return Result.error(400, "参数错误：localPath 不能为空！");
        }

        try {
            // 3. 调用业务层，拿到这批图片在阿里云 OSS 的终极统一链接列表
            List<String> imageUrls = productImageService.uploadAndSyncImages(productId, localPath, sort);

            // 4. 动态拼装符合你要求的提示话术
            int successCount = imageUrls.size();
            String customMsg = "批量上传成功！一共成功同步了 " + successCount + " 张图片。";

            // 5. 组装高规格的标准化统一返回
            return Result.success(customMsg, imageUrls);

        } catch (Exception e) {
            // 如果 Service 层抛出了路径找不到或者 OSS 推送失败的异常，这里直接兜底捕获并返回 500
            return Result.error(500, "运行时异常：" + e.getMessage());
        }
    }
}
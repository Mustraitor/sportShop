package org.server.controller;

import org.server.service.ProductImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/oss")
public class ProductImageController {

    @Autowired
    private ProductImageService productImageService; // 注入规范的业务接口

    /**
     * 规范整改后的统一入口
     * GET http://localhost:8080/oss/upload-product?productId=1001&localPath=D:/images
     */
    @GetMapping("/upload-product")
    public String uploadProductImage(
            @RequestParam(value = "productId", required = false, defaultValue = "0") Long productId,
            @RequestParam("localPath") String localPath,
            @RequestParam(value = "sort", defaultValue = "1") Integer sort) {

        // 完美的职责分离：Controller 只负责把参数丢给后台，不关心它是怎么传给阿里云或怎么存数据库的
        return productImageService.uploadAndSyncImages(productId, localPath, sort);
    }
}
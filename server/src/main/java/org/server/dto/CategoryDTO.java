package org.server.dto;

import lombok.Data;

/**
 * 分类模块请求参数容器
 */
public class CategoryDTO {

    /**
     * 创建分类的请求参数
     */
    @Data
    public static class Create {
        private String name;
        private Long parentId;
        private Integer sort;
    }
    @Data
    public static class Update {
        private Long id;          // 必填，目标分类ID
        private String name;
        private Long parentId;    // 拟修改的父分类ID
        private Integer sort;
    }
}
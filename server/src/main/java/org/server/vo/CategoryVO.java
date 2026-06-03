package org.server.vo;

import lombok.Data;
import java.util.List;

@Data
public class CategoryVO {
    private Long id;
    private String name;
    private Long parentId;
    private Integer sort;

    // 递归的核心：嵌套自身的列表
    private List<CategoryVO> children;
    @Data
    public static class Simple {
        private Long id;
        private String name;
        private Long parentId;
        private Integer sort;
    }
}
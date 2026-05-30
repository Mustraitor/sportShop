package org.server.vo;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class PageResult<T> {
    private Long total;
    private Integer page;
    private Integer size;
    private Integer pages;
    private List<T> list;

    public PageResult(Long total, Integer page, Integer size, List<T> list) {
        this.total = total;
        this.page = page;
        this.size = size;
        // 自动计算总页数
        this.pages = (int) Math.ceil((double) total / size);
        this.list = list;
    }
}
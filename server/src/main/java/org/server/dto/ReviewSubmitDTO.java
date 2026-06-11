package org.server.dto;

import jakarta.validation.constraints.*;
import lombok.Data;


@Data
public class ReviewSubmitDTO {
    @NotNull(message = "商品ID不能为空")
    private Long productId;

    @NotNull(message = "评分不能为空")
    @Min(1) @Max(5)
    private Integer rating;

    @NotBlank(message = "评价内容不能为空")
    @Size(max = 500, message = "评价内容最多500字")
    private String content;
}
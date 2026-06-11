package org.server.controller;

import org.server.common.Result;
import org.server.dto.ReviewSubmitDTO;
import org.server.service.ReviewService;
import org.server.utils.BaseContext;
import org.server.vo.PageResult;
import org.server.vo.ReviewVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Min;

@RestController
@RequestMapping("/review")
@Validated
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    @PostMapping
    public Result<ReviewVO> submitReview(@Valid @RequestBody ReviewSubmitDTO dto) {
        Long userId = BaseContext.getCurrentId();
        ReviewVO vo = reviewService.submitReview(dto, userId);
        return Result.success("评价成功", vo);
    }

    @GetMapping("/product/{id}")
    public Result<PageResult<ReviewVO>> getProductReviews(
            @PathVariable("id") Long productId,
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @RequestParam(defaultValue = "10") @Min(1) Integer size) {
        PageResult<ReviewVO> result = reviewService.getProductReviews(productId, page, size);
        return Result.success(result);
    }
}
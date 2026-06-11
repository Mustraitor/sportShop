package org.server.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.server.dto.ReviewSubmitDTO;
import org.server.entity.Review;
import org.server.vo.PageResult;
import org.server.vo.ReviewVO;

public interface ReviewService extends IService<Review> {
    ReviewVO submitReview(ReviewSubmitDTO dto, Long userId);
    PageResult<ReviewVO> getProductReviews(Long productId, Integer page, Integer size);
}
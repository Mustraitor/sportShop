package org.server.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.server.dto.ReviewSubmitDTO;
import org.server.entity.Review;
import org.server.mapper.ReviewMapper;
import org.server.mapper.UserMapper;
import org.server.service.ReviewService;
import org.server.vo.PageResult;
import org.server.vo.ReviewVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class ReviewServiceImpl extends ServiceImpl<ReviewMapper, Review> implements ReviewService {

    @Autowired
    private UserMapper userMapper;

    @Override
    @Transactional
    public ReviewVO submitReview(ReviewSubmitDTO dto, Long userId) {
        Review review = new Review();
        review.setUserId(userId);
        review.setProductId(dto.getProductId());
        review.setRating(dto.getRating());
        review.setContent(dto.getContent());
        this.save(review);

        String userName = userMapper.selectUserNameById(userId);

        ReviewVO vo = new ReviewVO();
        vo.setId(review.getId());
        vo.setUserName(userName);
        vo.setRating(review.getRating());
        vo.setContent(review.getContent());
        vo.setCreatedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        return vo;
    }

    @Override
    public PageResult<ReviewVO> getProductReviews(Long productId, Integer page, Integer size) {
        // 使用 MyBatis-Plus 分页查询
        Page<ReviewVO> pageParam = new Page<>(page, size);
        IPage<ReviewVO> iPage = baseMapper.selectReviewVOPage(pageParam, productId);
        // 直接使用 PageResult 的全参构造器（不修改原有类）
        return new PageResult<>(
                iPage.getTotal(),
                (int) iPage.getCurrent(),
                (int) iPage.getSize(),
                iPage.getRecords()
        );
    }
}
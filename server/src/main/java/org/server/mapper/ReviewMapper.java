package org.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.server.entity.Review;
import org.server.vo.ReviewVO;

@Mapper
public interface ReviewMapper extends BaseMapper<Review> {

    @Select("SELECT r.id, u.user_name AS userName, r.rating, r.content, " +
            "DATE_FORMAT(r.created_at, '%Y-%m-%d %H:%i:%s') AS createdAt " +
            "FROM review r " +
            "JOIN sys_user u ON r.user_id = u.user_id " +
            "WHERE r.product_id = #{productId} " +
            "ORDER BY r.created_at DESC")
    IPage<ReviewVO> selectReviewVOPage(Page<?> page, @Param("productId") Long productId);
}
package org.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.server.entity.Cart;

@Mapper // 🌟 让 Spring 顺利识别
public interface CartMapper extends BaseMapper<Cart> {
}
package org.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.server.entity.Category;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CategoryMapper extends BaseMapper<Category> {
}
package org.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.server.entity.Product;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {
    // 里面空空如也！不需要写任何方法，父类已经帮你自带了 selectById 等几十个方法
}
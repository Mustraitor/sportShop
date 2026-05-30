package org.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.server.entity.ProductSku;

@Mapper
public interface ProductSkuMapper extends BaseMapper<ProductSku> {
    // 留空即可，BaseMapper 已经自带了所有的单表增删改查方法
}
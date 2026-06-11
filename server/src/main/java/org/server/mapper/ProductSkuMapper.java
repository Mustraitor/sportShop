package org.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.server.entity.ProductSku;

@Mapper
public interface ProductSkuMapper extends BaseMapper<ProductSku> {
    /**
     * 核心逻辑：原子性扣减库存
     *
     * 1. 为什么用原生 SQL？
     *    因为数据库更新操作是原子的。通过在 WHERE 子句中加入 stock >= #{quantity}，
     *    可以保证即使在极高并发下，库存也不会被扣成负数。
     *
     * 2. 返回值 int 的作用：
     *    如果更新成功（库存充足），返回 1；
     *    如果更新失败（库存不足导致 WHERE 条件不成立），返回 0。
     */
    @Update("UPDATE product_sku " +
            "SET stock = stock - #{quantity} " +
            "WHERE id = #{skuId} " +
            "AND stock >= #{quantity} " +
            "AND is_deleted = 0")
    int decreaseStock(@Param("skuId") Long skuId, @Param("quantity") Integer quantity);
    /**
     * 追加方法：回滚/增加库存（用于取消订单）
     */
    @Update("UPDATE product_sku SET stock = stock + #{quantity} WHERE id = #{id}")
    int increaseStock(@Param("id") Long id, @Param("quantity") Integer quantity);
}
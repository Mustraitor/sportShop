package org.server.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class CartVO {
    private Integer total;                  // 购物车商品种类数量
    private BigDecimal checkedTotalAmount;  // 已勾选商品总金额
    private List<CartItemVO> list;          // 🌟 这里的泛型同步改成了 CartItemVO
}
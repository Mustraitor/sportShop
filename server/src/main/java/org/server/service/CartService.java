package org.server.service;

import org.server.dto.CartDTO;
import org.server.vo.CartVO;

public interface CartService {

    /**
     * 查看购物车列表
     */
    CartVO getCartList(Long userId, String guestId);

    /**
     * 添加商品到购物车
     */
    Long addToCart(Long userId, String guestId, CartDTO.CartAddDTO cartAddDTO);

    /**
     * 步进更新购物车数量（通过 action 控制加减一）
     * 💡 这里的 cartId 在 Redis 架构下传入的就是 skuId
     */
    void updateCartStep(Long userId, String guestId, Long cartId, String action);

    /**
     * 从购物车中删除某项商品（纯内存快速删除）
     */
    void deleteCartItem(Long userId, String guestId ,Long cartId);

    /**
     * 修改购物车商品勾选状态
     */
    void updateCartChecked(Long userId,String guestId, Long cartId, Integer checked);

    /**
     * 清空购物车
     */
    void clearCart(Long userId, String guestId);

    public void mergeCartAfterLogin(Long userId, String guestId);
}
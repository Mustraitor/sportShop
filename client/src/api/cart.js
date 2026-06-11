import request from '@/utils/request'; // 假设这是你封装好的 Axios 实例

export const cartApi = {
  // 1. 查看购物车
  getCartList() {
    return request({
      url: '/cart/list',
      method: 'GET'
    });
  },

  // 2. 添加到购物车
  addToCart(data) {
    // data 包含: { productId, skuId, quantity, guestId(可选) }
    return request({
      url: '/cart',
      method: 'POST',
      data
    });
  },

  // 3. 更新购物车数量 (加一个或减一个)
  // action: 'add' 或 'sub'
  updateCartQuantity(skuId, action) {
    return request({
      url: `/cart/${skuId}`,
      method: 'PUT',
      data: { action }
    });
  },

  // 4. 删除购物车中的某项
  deleteCartItem(skuId) {
    return request({
      url: `/cart/${skuId}`,
      method: 'DELETE'
    });
  },

  // 5. 勾选/取消勾选
  // checked: 1 或 0
  toggleCheckItem(skuId, checked) {
    return request({
      url: `/cart/checked/${skuId}`,
      method: 'PUT',
      data: { checked }
    });
  },

  // 6. 清空购物车
  clearCart() {
    return request({
      url: '/cart/clear',
      method: 'DELETE'
    });
  }
};
// src/api/order.js
import request from '@/utils/request';

export const orderApi = {
  // ================== 状态/样式映射辅助 ==================
  
  /**
   * 订单状态文本映射
   * 后端状态: 0=待支付, 1=待收货, 2=已取消, 3=已完成
   */
  getStatusText(status) {
    const map = {
      0: '待付款',
      1: '待收货', 
      2: '已取消',
      3: '已完成'
    };
    return map[status] || '未知';
  },

  /**
   * 订单状态对应的样式类名
   */
  getStatusClass(status) {
    const map = {
      0: 'status-pending',
      1: 'status-shipped', 
      2: 'status-cancelled',
      3: 'status-completed'
    };
    return map[status] || '';
  },

  // ================== 真实后端 API 对接 ==================

  /**
   * 1. 创建订单 (对应 OrderDTO.Create)
   * data 包含: { addressId, cartIds: [] }
   */
  createOrder(data) {
    return request({
      url: '/order',
      method: 'POST',
      data
    });
  },

  /**
   * 2. 获取订单列表（分页）
   * params 包含: { page, pageSize, status(可选) }
   */
  getOrderList(params) {
    return request({
      url: '/order/page',
      method: 'GET',
      params
    });
  },

  /**
   * 3. 获取订单详情
   */
  getOrderDetail(orderId) {
    return request({
      url: `/order/${orderId}`,
      method: 'GET'
    });
  },

  /**
   * 4. 支付订单 (对应 PaymentDTO)
   * data 包含: { orderId, paymentMethod }
   */
  payOrder(data) {
    return request({
      url: '/order/payment',
      method: 'POST',
      data
    });
  },

  /**
   * 5. 取消订单
   */
  cancelOrder(orderId) {
    return request({
      url: `/order/${orderId}/cancel`,
      method: 'PUT'
    });
  },

  /**
   * 6. 确认收货
   */
  confirmReceipt(orderId) {
    return request({
      url: `/order/${orderId}/receive`,
      method: 'PUT'
    });
  },

  /**
   * 7. 订单发货 (对应 OrderDTO.ShipReq，后台商家端使用)
   * data 包含: { trackingNumber, company }
   */
  shipOrder(orderId, data) {
    return request({
      url: `/order/${orderId}/ship`,
      method: 'PUT',
      data
    });
  },

  /**
   * 8. 修改待付款订单的收货地址 (对应 OrderDTO.UpdateAddress)
   * data 包含: { orderId, addressId }
   */
  updateOrderAddress(data) {
    return request({
      url: '/order/address',
      method: 'PUT',
      data
    });
  },

  /**
   * 删除订单
   */
  deleteOrder(orderId) {
    return request({
      url: `/order/${orderId}`,
      method: 'delete'
    })
  },

  /**
   * 再次购买 (当前模拟返回)
   */
  rebuyOrder(orderId) {
    console.warn('再次购买应结合购物车业务，当前为模拟返回');
    return Promise.resolve({ code: 200, msg: '模拟加入购物车成功', success: true });
  },

  /**
   * 获取未读消息数量 (当前模拟返回)
   */
  getUnreadCount() {
    return Promise.resolve({ count: 0 });
  }
};
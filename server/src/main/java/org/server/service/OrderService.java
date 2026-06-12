package org.server.service;

import org.server.dto.OrderDTO;
import org.server.dto.PaymentDTO;
import org.server.vo.OrderVO;

public interface OrderService {

    /**
     * 从购物车创建订单
     * @param userId 登录用户主键
     * @param req 地址和购物车项参数
     * @return 包含订单号、总价、应付总额的VO对象
     */
    OrderVO.Create createOrderFromCart(Long userId, OrderDTO.Create req);
    /**
     * 根据订单ID获取订单详情
     * @param userId 当前登录用户ID（安全防御，防止越权看别人的订单）
     * @param orderId 订单ID
     */
    OrderVO.Detail getOrderDetail(Long userId, Long orderId);
    /**
     * 分页查询当前用户的订单列表
     */
    OrderVO.PageResult getOrderPage(Long userId, OrderDTO.PageReq req);
    /**
     * 模拟支付接口
     */
    void payOrder(Long userId, PaymentDTO paymentDTO);
    /**
     * 取消订单
     * @param userId  当前登录用户ID
     * @param orderId 订单ID
     * @return 取消结果 VO
     */
    OrderVO.CancelResult cancelOrder(Long userId, Long orderId);
    /**
     * 订单发货
     * @param orderId 订单ID
     * @param req     发货物流信息DTO（包含快递单号和快递公司）
     * @return 发货结果 VO
     */
    OrderVO.ShipResult shipOrder(Long orderId, OrderDTO.ShipReq req);

    /**
     * 用户确认收货
     * @param userId  当前登录用户ID（用于防越权校验）
     * @param orderId 订单ID
     * @return 确认收货结果 VO
     */
    OrderVO.ReceiveResult receiveOrder(Long userId, Long orderId);

    void deleteOrder(Long userId, Long orderId);

    /**
     * 修改待付款订单的收货地址
     * @param updateReq 包含订单ID和新地址ID的请求体
     * @return 是否修改成功
     */
    boolean updateOrderAddress(OrderDTO.UpdateAddress updateReq);

}

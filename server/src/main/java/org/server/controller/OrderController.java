package org.server.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.server.common.Result;
import org.server.dto.OrderDTO;
import org.server.dto.PaymentDTO;
import org.server.service.OrderService;
import org.server.utils.BaseContext;
import org.server.vo.OrderVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * POST /order 创建订单
     */
    @PostMapping
    public Result<OrderVO.Create> createOrder(@Validated @RequestBody OrderDTO.Create req) {

        Long userId = (Long) BaseContext.getCurrentId();
        // 调用业务层
        OrderVO.Create data = orderService.createOrderFromCart(userId, req);
        // 统一格式返回
        return Result.success("订单创建成功", data);
    }
    @GetMapping("/{id}")
    public Result<OrderVO.Detail> getOrderDetail(@PathVariable("id") Long orderId) {

        Long userId = BaseContext.getCurrentId();
        // 调用业务层获取聚合后的详情数据
        OrderVO.Detail data = orderService.getOrderDetail(userId, orderId);
        // 返回统一规范格式
        return Result.success("订单详情查询成功", data);
    }
    /**
     * GET /order/page 分页查询订单列表
     */
    @GetMapping("/page")
    public Result<OrderVO.PageResult> getOrderPage(@Validated OrderDTO.PageReq req) {
        Long userId = BaseContext.getCurrentId();
        OrderVO.PageResult data = orderService.getOrderPage(userId, req);
        return Result.success("订单列表查询成功", data);
    }
    @PostMapping("/payment")
    public Result<Void> payOrder(@Validated @RequestBody PaymentDTO paymentDTO) {
        // 保持用户 ID 一致
        Long userId = BaseContext.getCurrentId();

        // 调用业务层执行扣款和改状态
        orderService.payOrder(userId, paymentDTO);

        return Result.success("支付成功", null);
    }
    @PutMapping("/{id}/cancel")
    public Result<OrderVO.CancelResult> cancelOrder(
            @PathVariable("id") @NotNull(message = "订单ID不能为空") Long orderId) {

        Long userId = BaseContext.getCurrentId();
        // 直接把 id 传给 service 层
        OrderVO.CancelResult result = orderService.cancelOrder(userId, orderId);

        return Result.success("订单取消成功", result);
    }
    /**
     * PUT /order/{id}/ship 订单发货
     */
    @PutMapping("/{id}/ship")
    public Result<OrderVO.ShipResult> shipOrder(
            @PathVariable("id") Long orderId,
            @Valid @RequestBody OrderDTO.ShipReq req) {

        // 发货通常是后台管理员或商家操作，这里传入操作人或直接执行
        OrderVO.ShipResult result = orderService.shipOrder(orderId, req);
        return Result.success("发货成功", result);
    }

    /**
     * PUT /order/{id}/receive 确认收货
     */
    @PutMapping("/{id}/receive")
    public Result<OrderVO.ReceiveResult> receiveOrder(@PathVariable("id") Long orderId) {
        // 确认收货是用户操作，需要防越权
        Long userId = BaseContext.getCurrentId();
        OrderVO.ReceiveResult result = orderService.receiveOrder(userId, orderId);
        return Result.success("确认收货成功", result);
    }
    /**
     * DELETE /order/{id} 删除订单（逻辑删除）
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteOrder(
            @PathVariable("id") @NotNull(message = "订单ID不能为空") Long orderId) {
        Long userId = BaseContext.getCurrentId();
        orderService.deleteOrder(userId, orderId);
        return Result.success("订单删除成功", null);
    }
    @PutMapping("/address")
    public Result<?> updateAddress(@Validated @RequestBody OrderDTO.UpdateAddress updateReq) {
        // 业务层处理，这里会传入 orderId 和 addressId
        orderService.updateOrderAddress(updateReq);
        return Result.success("订单地址修改成功");
    }
}
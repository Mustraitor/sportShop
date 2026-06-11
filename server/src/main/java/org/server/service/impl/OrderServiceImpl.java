package org.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.server.common.constant.OrderStatus;
import org.server.common.exception.BusinessException;
import org.server.dto.OrderDTO;
import org.server.dto.PaymentDTO;
import org.server.entity.*;
import org.server.mapper.*;
import org.server.service.OrderService;
import org.server.vo.OrderVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrdersMapper ordersMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private ProductSkuMapper productSkuMapper;

    @Autowired
    private AddressMapper addressMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private OrderShippingMapper orderShippingMapper;

    @Autowired
    private PaymentMapper paymentMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // 严格与你的 CartServiceImpl 保持一致
    private static final String CART_KEY_PREFIX = "cart:";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO.Create createOrderFromCart(Long userId, OrderDTO.Create req) {
        // 数组绝对不能为空
        if (CollectionUtils.isEmpty(req.getCartIds())) {
            throw new BusinessException(400, "请至少选择一件商品进行结算");
        }

        // 利用 Set 强行去重，彻底封死重复 ID 攻击！
        Set<Long> uniqueSkuIds = req.getCartIds().stream().collect(Collectors.toSet());
        // 1. 验证收货地址
        Address address = addressMapper.selectById(req.getAddressId());
        if (address == null || address.getIsDeleted() == 1) {
            throw new BusinessException(404, "收货地址不存在，请重新选择");
        }
        if (!address.getUserId().equals(userId)) {
            throw new BusinessException(403, "非法操作：该地址不属于当前用户");
        }

        // 2. 从 Redis 读取该用户的购物车全量哈希数据
        String redisKey = CART_KEY_PREFIX + userId;
        Map<Object, Object> redisCartMap = redisTemplate.opsForHash().entries(redisKey);

        if (CollectionUtils.isEmpty(redisCartMap)) {
            throw new BusinessException(400, "购物车为空或记录已失效");
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItemsToSave = new ArrayList<>();
        List<String> fieldsToRemove = new ArrayList<>();

        // 当前统一的下单时间线
        LocalDateTime now = LocalDateTime.now();

        // 3. 遍历前端传来的 cartIds (skuId)
        for (Long skuId : req.getCartIds()) {

            // 从 Redis 中抽取出对应的购物车项
            Object rawItem = redisCartMap.get(skuId.toString());
            if (rawItem == null) {
                throw new BusinessException(400, "部分商品在购物车中已不存在，请刷新页面");
            }

            // 转换并拿到 Redis 中的购买数量
            RedisCartItem cartItem = (RedisCartItem) rawItem;

            // 防止恶意调用：只有在购物车中处于勾选状态(比如 1-勾选)的商品才能下单
            if (cartItem.getChecked() == null || cartItem.getChecked() != 1) {
                throw new BusinessException(400, "包含未勾选的商品，无法提交订单");
            }

            Integer quantity = cartItem.getQuantity();
            if (quantity == null || quantity <= 0) {
                throw new BusinessException(400, "商品数量异常，请重新加入购物车");
            }
            // 4. 严格校验商品与 SKU 数据状态
            ProductSku sku = productSkuMapper.selectOne(new LambdaQueryWrapper<ProductSku>()
                    .eq(ProductSku::getId, skuId)
                    .eq(ProductSku::getIsDeleted, 0));
            if (sku == null) {
                throw new BusinessException(400, "选中的部分商品规格已失效");
            }

            Product product = productMapper.selectById(sku.getProductId());
            if (product == null || product.getStatus() != 1 || product.getIsDeleted() != 0) {
                throw new BusinessException(400, "商品 [" + (product != null ? product.getName() : "未知") + "] 已下架");
            }

            // 5. 核心并发防御：数据库乐观锁扣减库存
            int affectedRows = productSkuMapper.decreaseStock(sku.getId(), quantity);
            if (affectedRows == 0) {
                throw new BusinessException(500, "商品 [" + product.getName() + " - " + sku.getSkuName() + "] 库存不足，下单失败");
            }

            // 计算单品总额
            BigDecimal itemTotalPrice = sku.getPrice().multiply(new BigDecimal(quantity));
            totalAmount = totalAmount.add(itemTotalPrice);

            // 组装订单项快照
            OrderItem orderItem = new OrderItem();
            orderItem.setProductId(product.getId());
            orderItem.setSkuId(sku.getId());
            orderItem.setProductName(product.getName());
            orderItem.setSkuName(sku.getSkuName());
            orderItem.setPrice(sku.getPrice());
            orderItem.setQuantity(quantity);
            orderItem.setTotalPrice(itemTotalPrice);

            // 回填详情表快照时间
            orderItem.setCreatedAt(now);

            orderItemsToSave.add(orderItem);
            fieldsToRemove.add(skuId.toString());
        }

        // 6. 订单主表记录落库 (Orders)
        Orders order = new Orders();
        order.setUserId(userId);
        order.setAddressId(req.getAddressId());
        order.setTotalAmount(totalAmount);
        order.setPayAmount(totalAmount);
        order.setStatus(0); // 0-待支付
//        order.setPaymentType(1); // 默认1

        // 死卡时间 Null Bug，给主表硬核赋上创建与更新时间
        order.setCreatedAt(now);
        order.setUpdatedAt(now);

        ordersMapper.insert(order);

        // 7. 批量回填 orderId 并写入详情表 (OrderItem)
        for (OrderItem item : orderItemsToSave) {
            item.setOrderId(order.getId());
            orderItemMapper.insert(item);
        }

        // 8. 清理 Redis 缓存
        if (!fieldsToRemove.isEmpty()) {
            redisTemplate.opsForHash().delete(redisKey, fieldsToRemove.toArray());
        }

        // 9. 组装最终结果返回给 Controller
        OrderVO.Create response = new OrderVO.Create();
        response.setOrderId(order.getId());
        response.setTotalAmount(order.getTotalAmount());
        response.setPayAmount(order.getPayAmount());

        return response;
    }

    @Override
    public OrderVO.Detail getOrderDetail(Long userId, Long orderId) {
        // 1. 查询订单主表记录
        Orders order = ordersMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }

        // 2. 越权硬防御：校验该订单是否属于当前登录用户
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(403, "非法操作：无权查看他人订单");
        }

        // 3. 关联查询订单明细表（从 order_item 快照表中捞出该订单的所有商品）
        List<OrderItem> orderItems = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>()
                        .eq(OrderItem::getOrderId, orderId)
        );

        // 4. 组装 VO 返回对象
        OrderVO.Detail detail = new OrderVO.Detail();
        detail.setId(order.getId());
        detail.setUserId(order.getUserId());
        detail.setAddressId(order.getAddressId());
        detail.setTotalAmount(order.getTotalAmount());
        detail.setPayAmount(order.getPayAmount());
        detail.setStatus(order.getStatus());
//        detail.setPaymentType(order.getPaymentType());
        if (order.getStatus() >= OrderStatus.PENDING_RECEIVE.getCode()) {
            Payment payment = paymentMapper.selectOne(new LambdaQueryWrapper<Payment>()
                    .eq(Payment::getOrderId, orderId)
                    .eq(Payment::getStatus, (byte) 1) // 查支付成功的流水
                    .last("LIMIT 1")); // 加上 LIMIT 1 提升查询性能

            if (payment != null) {
                detail.setPaymentType(payment.getPaymentMethod().intValue());
            }
        }
        detail.setCreatedAt(order.getCreatedAt());

        // 注入明细列表
        detail.setItems(orderItems);

        return detail;
    }

    @Override
    public OrderVO.PageResult getOrderPage(Long userId, OrderDTO.PageReq req) {
        // 1. 构建分页参数
        Page<Orders> pageParam = new Page<>(req.getPage(), req.getPageSize());

        // 2. 组装查询条件
        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<Orders>()
                .eq(Orders::getUserId, userId)
                .eq(Orders::getIsDeleted, 0)
                .orderByDesc(Orders::getCreatedAt);

        if (req.getStatus() != null) {
            queryWrapper.eq(Orders::getStatus, req.getStatus());
        }

        // 3. 执行分表查询
        Page<Orders> ordersPage = ordersMapper.selectPage(pageParam, queryWrapper);

        // 4. 初始化返回结果对象
        OrderVO.PageResult result = new OrderVO.PageResult();
        result.setTotal(ordersPage.getTotal());

        // 💡 核心补全：如果你的前端需要 totalPages 字段，在这里为它精准赋值
        // result.setPages(ordersPage.getPages());

        // 如果查出来是空列表，直接返回，避免去查明细表导致 IN() 报错
        if (ordersPage.getRecords().isEmpty()) {
            result.setList(new ArrayList<>());
            return result;
        }

        // 5. 提取当前页的所有订单 ID
        List<Long> orderIds = ordersPage.getRecords().stream()
                .map(Orders::getId)
                .collect(Collectors.toList());

        // 6. 批量捞出对应的商品明细快照
        List<OrderItem> allOrderItems = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().in(OrderItem::getOrderId, orderIds)
        );

        // 7. 按订单 ID 进行分组整理
        Map<Long, List<OrderItem>> itemMap = allOrderItems.stream()
                .collect(Collectors.groupingBy(OrderItem::getOrderId));

        // 8. 循环装配每一个订单卡片所需要的数据
        List<OrderVO.OrderListItem> listItems = new ArrayList<>();
        for (Orders order : ordersPage.getRecords()) {
            OrderVO.OrderListItem listItem = new OrderVO.OrderListItem();
            listItem.setId(order.getId());
            listItem.setStatus(order.getStatus());
            listItem.setTotalAmount(order.getTotalAmount());
            // 💡 确保把 payAmount 也塞给前端，因为你的 OrderCard 里面用到了 order.payAmount
//            listItem.setPayAmount(order.getPayAmount());
            listItem.setCreatedAt(order.getCreatedAt());

            // 提取并精简商品字段
            List<OrderItem> orderItems = itemMap.getOrDefault(order.getId(), new ArrayList<>());
            List<OrderVO.ItemMin> minItems = orderItems.stream().map(item -> {
                OrderVO.ItemMin min = new OrderVO.ItemMin();
                min.setProductName(item.getProductName());
                min.setSkuName(item.getSkuName());
                min.setQuantity(item.getQuantity());
                // 💡 补充：把图片、价格也带上，防止前端列表初始化时缺失快照
//                min.setPrice(item.getPrice());
                return min;
            }).collect(Collectors.toList());

            listItem.setItems(minItems);
            listItems.add(listItem);
        }

        result.setList(listItems);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void payOrder(Long userId, PaymentDTO paymentDTO) {
        // 1. 查询订单
        Orders order = ordersMapper.selectById(paymentDTO.getOrderId());

        // 2. 基础与状态校验
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        if (order.getPayAmount() == null || order.getPayAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(400, "订单金额异常，拒绝支付");
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权操作他人订单");
        }
        // 建议这里用上你之前的枚举类，写成 OrderStatus.PENDING_PAY.getCode() 更规范，数字 0 也行
        if (!order.getStatus().equals(OrderStatus.PENDING_PAY.getCode())) {
            throw new BusinessException(400, "订单状态异常，无法支付");
        }

        // 3. 核心逻辑：从用户钱包扣钱（你之前写的这段利用底层 SQL 防超卖的设计非常赞 👏）
        int rows = userMapper.decreaseBalance(userId, order.getPayAmount());
        if (rows == 0) {
            throw new BusinessException(400, "账户余额不足");
        }

        // 4.扣钱成功后，立刻填充并写入 payment 支付流水表！
        Payment payment = new Payment();
        payment.setOrderId(order.getId());
        payment.setUserId(userId);
        payment.setAmount(order.getPayAmount()); // 忠实记录支付金额
        Integer method = paymentDTO.getPaymentMethod();
        payment.setPaymentMethod(method != null ? method.byteValue() : (byte) 1);

        // 生成一个高大上的内部模拟流水号（符合你的 trade_no VARCHAR(100) 设计）
        String fakeTradeNo = "PAY_" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
        payment.setTradeNo(fakeTradeNo);
        payment.setStatus((byte) 1); // 1 代表支付成功（正好对应你表里的 TINYINT）
        payment.setCreatedAt(LocalDateTime.now());

        paymentMapper.insert(payment); // 支付记录正式落库！

        // 5. 扣钱并记账成功后，修改订单状态为 1 (待收货)
        order.setStatus(OrderStatus.PENDING_RECEIVE.getCode()); // 改为 1
//        order.setPaymentType(paymentDTO.getPaymentMethod());

//        int updateOrderRows = ordersMapper.updateById(order);
        // 替换为 CAS 状态机防御：
        int updateOrderRows = ordersMapper.update(null, new LambdaUpdateWrapper<Orders>()
                .eq(Orders::getId, order.getId())
                .eq(Orders::getStatus, OrderStatus.PENDING_PAY.getCode()) // SQL 更新时必须再次校验当前状态是 0！
                .set(Orders::getStatus, OrderStatus.PENDING_RECEIVE.getCode())
                .set(Orders::getUpdatedAt, LocalDateTime.now()));

        if (updateOrderRows == 0) {
            throw new BusinessException(500, "订单状态已变更，支付失败，资金已拦截");
        }

    }

    @Override
    @Transactional(rollbackFor = Exception.class) // 涉及状态修改与库存回滚，必须开启事务
    public OrderVO.CancelResult cancelOrder(Long userId, Long orderId) {
        // 1. 查询订单主表
        Orders order = ordersMapper.selectById(orderId);

        // 2. 基础校验与防越权硬防御
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(403, "非法操作：无权取消他人订单");
        }

        // 3. 核心业务校验：只有状态为“0（待支付）”的订单才能被取消
        // 这里直接对照你之前定义的 org.server.common.constant.OrderStatus
        if (!order.getStatus().equals(OrderStatus.PENDING_PAY.getCode())) {
            throw new BusinessException(400, "当前订单状态无法取消（仅待支付订单可取消）");
        }

        // 4. 关联查询订单明细，准备回滚库存
        List<OrderItem> orderItems = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId)
        );

        // 5. 循环恢复商品 SKU 的库存
        if (!CollectionUtils.isEmpty(orderItems)) {
            for (OrderItem item : orderItems) {
                int rows = productSkuMapper.increaseStock(item.getSkuId(), item.getQuantity());
                if (rows == 0) {
                    throw new BusinessException(500, "回滚商品 [" + item.getProductName() + "] 库存失败");
                }
            }
        }

        // 6. 修改订单状态为 2 (已取消)
        order.setStatus(OrderStatus.CANCELLED.getCode());
        // 你的数据库建表有 ON UPDATE CURRENT_TIMESTAMP，但 MyBatis-Plus 默认更新需要实体属性或手动设置
        // 为了确保 VO 返回的时间精准，我们这里显式注入当前时间
        LocalDateTime now = LocalDateTime.now();
        order.setUpdatedAt(now);

//        int updateRows = ordersMapper.updateById(order);
        int updateRows = ordersMapper.update(null, new LambdaUpdateWrapper<Orders>()
                .eq(Orders::getId, order.getId())
                .eq(Orders::getStatus, OrderStatus.PENDING_PAY.getCode()) // 只能取消状态为 0 的
                .set(Orders::getStatus, OrderStatus.CANCELLED.getCode())
                .set(Orders::getUpdatedAt, now));

        if (updateRows == 0) {
            throw new BusinessException(500, "订单状态已改变，无法取消");
        }

        // 7. 组装最终结果返回给 Controller
        OrderVO.CancelResult result = new OrderVO.CancelResult();
        result.setOrderId(order.getId());
        result.setStatus(OrderStatus.CANCELLED.getCode());
        result.setStatusDescription(OrderStatus.CANCELLED.getDescription());
        result.setUpdatedAt(now);

        return result;
    }

    /**
     * 商家发货：防重复写入的幂等性版本
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO.ShipResult shipOrder(Long orderId, OrderDTO.ShipReq req) {
        // 1. 查询订单主表
        Orders order = ordersMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }

        // 2. 严格校验主表状态：只有用户已支付、处于 1 (待收货) 的订单，商家才能发货
        if (!order.getStatus().equals(OrderStatus.PENDING_RECEIVE.getCode())) {
            throw new BusinessException(400, "订单当前状态不是待收货/待发货状态");
        }

        // 3. 核心防重防御：先去物流表查一下，该订单之前到底有没有发过货
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OrderShipping> queryWrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        queryWrapper.eq(OrderShipping::getOrderId, orderId);
        OrderShipping existingShipping = orderShippingMapper.selectOne(queryWrapper);

        if (existingShipping == null) {
            // 情况 A：第一次发货，直接新建插入记录
            OrderShipping shipping = new OrderShipping();
            shipping.setOrderId(orderId);
            shipping.setCourierCompany(req.getCompany());
            shipping.setTrackingNo(req.getTrackingNumber());
            shipping.setStatus("SHIPPED");
            shipping.setShippedAt(LocalDateTime.now());
            orderShippingMapper.insert(shipping);
        } else {
            // 情况 B：已经发过货了（重复点击），执行更新覆盖，防止数据库爆炸！
            existingShipping.setCourierCompany(req.getCompany());
            existingShipping.setTrackingNo(req.getTrackingNumber());
            existingShipping.setShippedAt(LocalDateTime.now()); // 更新最新的发货时间
            orderShippingMapper.updateById(existingShipping);
        }

        // 4. 更新订单主表的更新时间
        order.setUpdatedAt(LocalDateTime.now());
        ordersMapper.updateById(order);

        // 5. 组装返回
        OrderVO.ShipResult result = new OrderVO.ShipResult();
        result.setId(order.getId());
        result.setStatus(OrderStatus.PENDING_RECEIVE.getCode());
        result.setCourierCompany(req.getCompany());
        result.setTrackingNo(req.getTrackingNumber());
        return result;
    }

    /**
     * 用户确认收货：修改订单主表状态为 3 (已完成)，联动更新物流表状态为 DELIVERED
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO.ReceiveResult receiveOrder(Long userId, Long orderId) {
        // 1. 查询订单主表
        Orders order = ordersMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }

        // 2. 防越权硬防御
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(403, "非法操作：无权操作他人订单");
        }

        // 3. 校验状态：只有当前是 1 (待收货) 状态的订单，用户才能确认收货
        if (!order.getStatus().equals(OrderStatus.PENDING_RECEIVE.getCode())) {
            throw new BusinessException(400, "当前订单状态无法确认收货");
        }

        // 4. 修改订单主表状态为 3 (已完成)
        order.setStatus(OrderStatus.COMPLETED.getCode());
        order.setUpdatedAt(LocalDateTime.now());
//        ordersMapper.updateById(order);
        int updateRows = ordersMapper.update(null, new LambdaUpdateWrapper<Orders>()
                .eq(Orders::getId, order.getId())
                .eq(Orders::getStatus, OrderStatus.PENDING_RECEIVE.getCode()) // SQL层面锁死，必须当前是 1 才能改成 3
                .set(Orders::getStatus, OrderStatus.COMPLETED.getCode())
                .set(Orders::getUpdatedAt, LocalDateTime.now()));

        if (updateRows == 0) {
            throw new BusinessException(400, "订单状态已变更，请勿重复操作确认收货");
        }

        // 5. 联动修改物流表：把物流状态改为已送达，并记录送达时间
        LambdaUpdateWrapper<OrderShipping> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(OrderShipping::getOrderId, orderId)
                .set(OrderShipping::getStatus, "DELIVERED")
                .set(OrderShipping::getDeliveredAt, LocalDateTime.now());
        orderShippingMapper.update(null, updateWrapper);

        // 6. 组装返回
        OrderVO.ReceiveResult result = new OrderVO.ReceiveResult();
        result.setId(order.getId());
        result.setStatus(OrderStatus.COMPLETED.getCode()); // 返回 3
        return result;
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteOrder(Long userId, Long orderId) {
        // 1. 查询订单主表（💡 注意：由于加了 @TableLogic，如果订单已经被删，此处会直接返回 null）
        Orders order = ordersMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(404, "订单不存在或已被删除");
        }

        // 2. 越权硬防御
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(403, "非法操作：无权删除他人订单");
        }

        // 3. 状态校验：只有 待付款(0)、已取消(2)、已完成(3) 的订单才可以删除
        int currentStatus = order.getStatus();
        if (currentStatus == 1) { // 待收货/发货中拦截
            throw new BusinessException(400, "订单正在运送或处理中，暂无法删除");
        }

        // 💡 特殊联动：如果是“待付款(0)”的订单用户直接点了删除，依然需要顺带把库存帮他回滚掉
        if (currentStatus == 0) {
            List<OrderItem> orderItems = orderItemMapper.selectList(
                    new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId)
            );
            if (!CollectionUtils.isEmpty(orderItems)) {
                for (OrderItem item : orderItems) {
                    int rows = productSkuMapper.increaseStock(item.getSkuId(), item.getQuantity());
                    if (rows == 0) {
                        throw new BusinessException(500, "回滚商品 [" + item.getProductName() + "] 库存失败");
                    }
                }
            }
        }

        // 4. 💡 核心修改：不改动原有 status 状态流转，直接将新字段 is_deleted 标记为 1
        int updateRows = ordersMapper.update(null, new LambdaUpdateWrapper<Orders>()
                .eq(Orders::getId, order.getId())
                .eq(Orders::getStatus, currentStatus) // CAS 状态机乐观锁防御
                .eq(Orders::getIsDeleted, 0)          // 防重复删除防御
                .set(Orders::getIsDeleted, 1)         // 💡 1-标记为已删除
                .set(Orders::getUpdatedAt, LocalDateTime.now()));

        if (updateRows == 0) {
            throw new BusinessException(500, "订单状态发生变更或已被他人删除，删除失败");
        }
    }
}
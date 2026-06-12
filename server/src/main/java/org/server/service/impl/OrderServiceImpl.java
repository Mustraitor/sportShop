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

            // 下单时，将商品的图片 URL 固化保存到明细表中
            orderItem.setMainImage(product.getMainImage());

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

        // 🎯【这次真的加上了！】下单落库时，必须把收货地址作为快照字段，牢牢写进 orders 表！
        order.setReceiverName(address.getName());
        order.setReceiverPhone(address.getPhone());
        order.setReceiverProvince(address.getProvince());
        order.setReceiverCity(address.getCity());
        order.setReceiverDistrict(address.getDistrict());
        order.setReceiverDetail(address.getDetail());

        // 给主表赋上创建与更新时间
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

        // 3. 关联查询订单明细表
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

        if (order.getStatus() >= OrderStatus.PENDING_RECEIVE.getCode()) {
            Payment payment = paymentMapper.selectOne(new LambdaQueryWrapper<Payment>()
                    .eq(Payment::getOrderId, orderId)
                    .eq(Payment::getStatus, (byte) 1)
                    .last("LIMIT 1"));

            if (payment != null) {
                detail.setPaymentType(payment.getPaymentMethod().intValue());
            }
        }
        detail.setCreatedAt(order.getCreatedAt());

        // 注入明细列表
        detail.setItems(orderItems);

        // =================================================================
        // 🎯 核心重构：双重防御组装收货人快照（彻底解决历史数据全是 null 导致的页面空白）
        // =================================================================
        OrderVO.ReceiverInfo receiverInfo = new OrderVO.ReceiverInfo();

        // 🔬 A 防线：如果数据库快照里是有值的（针对重构后下的新订单，或者修改过地址的订单）
        if (order.getReceiverName() != null && !order.getReceiverName().trim().isEmpty()) {
            receiverInfo.setName(order.getReceiverName());
            receiverInfo.setPhone(order.getReceiverPhone());

            // 拼接省、市、区、详细地址
            String fullAddress = (order.getReceiverProvince() != null ? order.getReceiverProvince() : "")
                    + (order.getReceiverCity() != null ? order.getReceiverCity() : "")
                    + (order.getReceiverDistrict() != null ? order.getReceiverDistrict() : "")
                    + (order.getReceiverDetail() != null ? order.getReceiverDetail() : "");
            receiverInfo.setAddress(fullAddress);
        }
        // 🔬 B 防线：如果快照字段全为 null（针对你数据库里那几条历史测试老订单）
        else {
            if (order.getAddressId() != null) {
                // 拿着 addressId 去实时地址表捞出来拼装，给老数据擦屁股
                Address address = addressMapper.selectById(order.getAddressId());
                if (address != null && address.getIsDeleted() == 0) {
                    receiverInfo.setName(address.getName());
                    receiverInfo.setPhone(address.getPhone());

                    String fullAddress = (address.getProvince() != null ? address.getProvince() : "")
                            + (address.getCity() != null ? address.getCity() : "")
                            + (address.getDistrict() != null ? address.getDistrict() : "")
                            + (address.getDetail() != null ? address.getDetail() : "");
                    receiverInfo.setAddress(fullAddress);
                } else {
                    receiverInfo.setName("未知收货人");
                    receiverInfo.setPhone("----");
                    receiverInfo.setAddress("关联的原始收货地址已被用户删除");
                }
            } else {
                receiverInfo.setName("未绑定地址");
                receiverInfo.setPhone("----");
                receiverInfo.setAddress("暂无地址信息");
            }
        }

        // 装配进 detail 传给前端
        detail.setReceiver(receiverInfo);

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

            // 🎯 核心修改 2：解开实付金额注释，完美供应给前端 OrderCard 组件
//            listItem.setPayAmount(order.getPayAmount());

            listItem.setCreatedAt(order.getCreatedAt());

            // 提取并精简商品字段
            List<OrderItem> orderItems = itemMap.getOrDefault(order.getId(), new ArrayList<>());
            List<OrderVO.ItemMin> minItems = orderItems.stream().map(item -> {
                OrderVO.ItemMin min = new OrderVO.ItemMin();
                min.setProductName(item.getProductName());
                min.setSkuName(item.getSkuName());
                min.setQuantity(item.getQuantity());

                // 🎯 核心修改 3：完美输出图片快照和真实单价给前端列表
                min.setMainImage(item.getMainImage());
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
        if (!order.getStatus().equals(OrderStatus.PENDING_PAY.getCode())) {
            throw new BusinessException(400, "订单状态异常，无法支付");
        }

        // 3. 核心逻辑：从用户钱包扣钱
        int rows = userMapper.decreaseBalance(userId, order.getPayAmount());
        if (rows == 0) {
            throw new BusinessException(400, "账户余额不足");
        }

        // 4.扣钱成功后，立刻填充并写入 payment 支付流水表
        Payment payment = new Payment();
        payment.setOrderId(order.getId());
        payment.setUserId(userId);
        payment.setAmount(order.getPayAmount());
        Integer method = paymentDTO.getPaymentMethod();
        payment.setPaymentMethod(method != null ? method.byteValue() : (byte) 1);

        String fakeTradeNo = "PAY_" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
        payment.setTradeNo(fakeTradeNo);
        payment.setStatus((byte) 1);
        payment.setCreatedAt(LocalDateTime.now());

        paymentMapper.insert(payment);

        // 5. CAS 状态机防御更新状态为 1 (待收货)
        int updateOrderRows = ordersMapper.update(null, new LambdaUpdateWrapper<Orders>()
                .eq(Orders::getId, order.getId())
                .eq(Orders::getStatus, OrderStatus.PENDING_PAY.getCode())
                .set(Orders::getStatus, OrderStatus.PENDING_RECEIVE.getCode())
                .set(Orders::getUpdatedAt, LocalDateTime.now()));

        if (updateOrderRows == 0) {
            throw new BusinessException(500, "订单状态已变更，支付失败，资金已拦截");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
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

        // 3. 核心业务校验
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

        // 6. CAS 状态机修改订单状态为 2 (已取消)
        LocalDateTime now = LocalDateTime.now();
        int updateRows = ordersMapper.update(null, new LambdaUpdateWrapper<Orders>()
                .eq(Orders::getId, order.getId())
                .eq(Orders::getStatus, OrderStatus.PENDING_PAY.getCode())
                .set(Orders::getStatus, OrderStatus.CANCELLED.getCode())
                .set(Orders::getUpdatedAt, now));

        if (updateRows == 0) {
            throw new BusinessException(500, "订单状态已改变，无法取消");
        }

        // 7. 组装最终结果
        OrderVO.CancelResult result = new OrderVO.CancelResult();
        result.setOrderId(order.getId());
        result.setStatus(OrderStatus.CANCELLED.getCode());
        result.setStatusDescription(OrderStatus.CANCELLED.getDescription());
        result.setUpdatedAt(now);

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO.ShipResult shipOrder(Long orderId, OrderDTO.ShipReq req) {
        Orders order = ordersMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }

        if (!order.getStatus().equals(OrderStatus.PENDING_RECEIVE.getCode())) {
            throw new BusinessException(400, "订单当前状态不是待收货/待发货状态");
        }

        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OrderShipping> queryWrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        queryWrapper.eq(OrderShipping::getOrderId, orderId);
        OrderShipping existingShipping = orderShippingMapper.selectOne(queryWrapper);

        if (existingShipping == null) {
            OrderShipping shipping = new OrderShipping();
            shipping.setOrderId(orderId);
            shipping.setCourierCompany(req.getCompany());
            shipping.setTrackingNo(req.getTrackingNumber());
            shipping.setStatus("SHIPPED");
            shipping.setShippedAt(LocalDateTime.now());
            orderShippingMapper.insert(shipping);
        } else {
            existingShipping.setCourierCompany(req.getCompany());
            existingShipping.setTrackingNo(req.getTrackingNumber());
            existingShipping.setShippedAt(LocalDateTime.now());
            orderShippingMapper.updateById(existingShipping);
        }

        order.setUpdatedAt(LocalDateTime.now());
        ordersMapper.updateById(order);

        OrderVO.ShipResult result = new OrderVO.ShipResult();
        result.setId(order.getId());
        result.setStatus(OrderStatus.PENDING_RECEIVE.getCode());
        result.setCourierCompany(req.getCompany());
        result.setTrackingNo(req.getTrackingNumber());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO.ReceiveResult receiveOrder(Long userId, Long orderId) {
        Orders order = ordersMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }

        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(403, "非法操作：无权操作他人订单");
        }

        if (!order.getStatus().equals(OrderStatus.PENDING_RECEIVE.getCode())) {
            throw new BusinessException(400, "当前订单状态无法确认收货");
        }

        int updateRows = ordersMapper.update(null, new LambdaUpdateWrapper<Orders>()
                .eq(Orders::getId, order.getId())
                .eq(Orders::getStatus, OrderStatus.PENDING_RECEIVE.getCode())
                .set(Orders::getStatus, OrderStatus.COMPLETED.getCode())
                .set(Orders::getUpdatedAt, LocalDateTime.now()));

        if (updateRows == 0) {
            throw new BusinessException(400, "订单状态已变更，请勿重复操作确认收货");
        }

        LambdaUpdateWrapper<OrderShipping> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(OrderShipping::getOrderId, orderId)
                .set(OrderShipping::getStatus, "DELIVERED")
                .set(OrderShipping::getDeliveredAt, LocalDateTime.now());
        orderShippingMapper.update(null, updateWrapper);

        OrderVO.ReceiveResult result = new OrderVO.ReceiveResult();
        result.setId(order.getId());
        result.setStatus(OrderStatus.COMPLETED.getCode());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteOrder(Long userId, Long orderId) {
        Orders order = ordersMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(404, "订单不存在或已被删除");
        }

        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(403, "非法操作：无权删除他人订单");
        }

        int currentStatus = order.getStatus();
        if (currentStatus == 1) {
            throw new BusinessException(400, "订单正在运送或处理中，暂无法删除");
        }

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

        int updateRows = ordersMapper.update(null, new LambdaUpdateWrapper<Orders>()
                .eq(Orders::getId, order.getId())
                .eq(Orders::getStatus, currentStatus)
                .eq(Orders::getIsDeleted, 0)
                .set(Orders::getIsDeleted, 1)
                .set(Orders::getUpdatedAt, LocalDateTime.now()));

        if (updateRows == 0) {
            throw new BusinessException(500, "订单状态发生变更或已被他人删除，删除失败");
        }
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateOrderAddress(OrderDTO.UpdateAddress updateReq) {
        // 1. 获取对应订单，校验是否存在以及是否已被逻辑删除
        Orders order = ordersMapper.selectById(updateReq.getOrderId());

        if (order == null || order.getIsDeleted() == 1) {
            throw new RuntimeException("该订单不存在或已被删除");
        }

        // 2. 状态校验：只有状态为 0 (待付款) 的订单才允许改地址
        if (order.getStatus() != 0) {
            throw new RuntimeException("订单已付款或已取消，无法修改地址");
        }

        // 🎯 3. 核心新增：去地址表查出用户新选择的地址详细信息
        Address newAddress = addressMapper.selectById(updateReq.getAddressId());
        if (newAddress == null) {
            throw new RuntimeException("选择的收货地址不存在");
        }

        // 4. 构造更新对象（只更新需要的字段，效率更高）
        Orders updateOrder = new Orders();
        updateOrder.setId(updateReq.getOrderId());

        // 💡 既更新来源 ID，又把最新地址的纯文本作为快照同步塞进订单主表
        updateOrder.setAddressId(updateReq.getAddressId());
        updateOrder.setReceiverName(newAddress.getName());
        updateOrder.setReceiverPhone(newAddress.getPhone());
        updateOrder.setReceiverProvince(newAddress.getProvince());
        updateOrder.setReceiverCity(newAddress.getCity());
        updateOrder.setReceiverDistrict(newAddress.getDistrict());
        updateOrder.setReceiverDetail(newAddress.getDetail());

        // 5. 执行更新
        int rows = ordersMapper.updateById(updateOrder);
        return rows > 0;
    }
}
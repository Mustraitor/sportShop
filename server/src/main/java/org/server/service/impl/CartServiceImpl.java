package org.server.service.impl;

import org.server.common.exception.BusinessException;
import org.server.dto.CartDTO;
import org.server.entity.Product;
import org.server.entity.ProductSku;
import org.server.entity.RedisCartItem;
import org.server.mapper.ProductMapper;
import org.server.mapper.ProductSkuMapper;
import org.server.service.CartService;
import org.server.vo.CartVO;
import org.server.vo.CartItemVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private ProductSkuMapper productSkuMapper;

    // 统一管理 Redis 前缀
    private static final String CART_KEY_PREFIX = "cart:";

    private String getCartRedisKey(Long userId, String guestId) {
        if (userId != null) {
            return "cart:" + userId;
        }
        return "cart:guest:" + guestId;
    }
    @Override
    public CartVO getCartList(Long userId, String guestId) {
        String redisKey = getCartRedisKey(userId, guestId);
        CartVO cartVO = new CartVO();
//        String redisKey = CART_KEY_PREFIX + userId;

        Map<Object, Object> redisCartMap = redisTemplate.opsForHash().entries(redisKey);

        if (redisCartMap == null || redisCartMap.isEmpty()) {
            cartVO.setTotal(0);
            cartVO.setCheckedTotalAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            cartVO.setList(new ArrayList<>());
            return cartVO;
        }

        List<Long> skuIds = redisCartMap.keySet().stream()
                .map(key -> Long.valueOf(key.toString()))
                .collect(Collectors.toList());

        List<ProductSku> skus = productSkuMapper.selectBatchIds(skuIds);
        if (skus == null || skus.isEmpty()) {
            cartVO.setTotal(0);
            cartVO.setCheckedTotalAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            cartVO.setList(new ArrayList<>());
            return cartVO;
        }

        Map<Long, ProductSku> skuMap = skus.stream().collect(Collectors.toMap(ProductSku::getId, s -> s));
        List<Long> productIds = skus.stream().map(ProductSku::getProductId).distinct().collect(Collectors.toList());
        Map<Long, Product> productMap = productMapper.selectBatchIds(productIds)
                .stream().collect(Collectors.toMap(Product::getId, p -> p));

        List<CartItemVO> list = new ArrayList<>();
        for (ProductSku sku : skus) {
            CartItemVO itemVO = new CartItemVO();
            itemVO.setId(sku.getId());
            itemVO.setProductId(sku.getProductId());
            itemVO.setSkuId(sku.getId());

            Object rawData = redisCartMap.get(sku.getId().toString());
            if (rawData instanceof RedisCartItem item) {
                itemVO.setQuantity(item.getQuantity());
                itemVO.setChecked(item.getChecked());
            } else {
                itemVO.setQuantity(1);
                itemVO.setChecked(1);
            }

            itemVO.setSkuName(sku.getSkuName());
            itemVO.setPrice(sku.getPrice());
            itemVO.setStock(sku.getStock());

            Product product = productMap.get(sku.getProductId());
            if (product != null) {
                itemVO.setProductName(product.getName());
                itemVO.setMainImage(product.getMainImage());
            }

            list.add(itemVO);
        }

        cartVO.setList(list);
        cartVO.setTotal(list.size());

        // 计算总金额时，只累加已勾选(checked == 1)的商品
        BigDecimal checkedTotalAmount = list.stream()
                .filter(item -> item.getChecked() != null && item.getChecked() == 1)
                .map(item -> {
                    BigDecimal price = item.getPrice() != null ? item.getPrice() : BigDecimal.ZERO;
                    BigDecimal qty = new BigDecimal(item.getQuantity() != null ? item.getQuantity() : 0);
                    return price.multiply(qty);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        cartVO.setCheckedTotalAmount(checkedTotalAmount.setScale(2, RoundingMode.HALF_UP));
        return cartVO;
    }

    @Override
    public Long addToCart(Long userId, String guestId, CartDTO.CartAddDTO cartAddDTO) {

        // ① 默认数量
        int addQuantity = cartAddDTO.getQuantity() == null ? 1 : cartAddDTO.getQuantity();

        // ② 查 SKU
        ProductSku sku = productSkuMapper.selectById(cartAddDTO.getSkuId());
        if (sku == null) {
            throw new BusinessException("商品规格不存在");
        }

        // ③ 获取 Redis Key（关键）
        String redisKey = getCartRedisKey(userId, guestId);
        String hashKey = cartAddDTO.getSkuId().toString();

        // ④ 读取旧数据
        RedisCartItem oldItem =
                (RedisCartItem) redisTemplate.opsForHash().get(redisKey, hashKey);

        int targetQuantity = addQuantity;

        // ⑤ 如果已存在 → 累加
        if (oldItem != null) {
            targetQuantity += oldItem.getQuantity();
        }

        // ⑥ 校验库存
        if (sku.getStock() == null || targetQuantity > sku.getStock()) {
            throw new BusinessException("库存不足");
        }

        // ⑦ 写入 Redis
        redisTemplate.opsForHash()
                .put(redisKey, hashKey, new RedisCartItem(targetQuantity, 1));

        return sku.getId();
    }

    @Override
    public void updateCartStep(Long userId,String guestId ,Long cartId, String action) {
//        String redisKey = CART_KEY_PREFIX + userId;
        String redisKey = getCartRedisKey(userId, guestId);
        String hashKey = cartId.toString();

        RedisCartItem oldItem = (RedisCartItem) redisTemplate.opsForHash().get(redisKey, hashKey);
        if (oldItem == null) throw new BusinessException("更新失败，购物车记录不存在");

        int currentQuantity = oldItem.getQuantity();
        int currentChecked = oldItem.getChecked();
        int targetQuantity;

        if ("add".equals(action)) {
            targetQuantity = currentQuantity + 1;
            ProductSku sku = productSkuMapper.selectById(cartId);
            if (sku == null || sku.getStock() == null || targetQuantity > sku.getStock()) {
                throw new BusinessException("商品库存不足");
            }
        } else {
            targetQuantity = currentQuantity - 1;
            if (targetQuantity < 1) throw new BusinessException("宝贝数量不能再减少了");
        }

        // 保持勾选状态
        redisTemplate.opsForHash().put(redisKey, hashKey, new RedisCartItem(targetQuantity, currentChecked));
    }

    @Override
    public void deleteCartItem(Long userId,String guestId ,Long cartId) {
//        String redisKey = CART_KEY_PREFIX + userId;
        String redisKey = getCartRedisKey(userId, guestId);
        String hashKey = cartId.toString();


        Boolean hasKey = redisTemplate.opsForHash().hasKey(redisKey, hashKey);
        if (Boolean.FALSE.equals(hasKey)) {
            throw new BusinessException("删除失败，该购物车记录不存在");
        }

        // 直接从用户的 Redis Hash 中抹去
        redisTemplate.opsForHash().delete(redisKey, hashKey);
    }

    @Override
    public void updateCartChecked(Long userId,String guestId, Long cartId, Integer checked) {
//        String redisKey = CART_KEY_PREFIX + userId;
        String redisKey = getCartRedisKey(userId, guestId);
        String hashKey = cartId.toString();

        RedisCartItem oldItem = (RedisCartItem) redisTemplate.opsForHash().get(redisKey, hashKey);
        if (oldItem == null) {
            throw new BusinessException("状态修改失败，该商品不在购物车中");
        }

        // 保持数量不变，更新勾选状态
        redisTemplate.opsForHash().put(redisKey, hashKey, new RedisCartItem(oldItem.getQuantity(), checked));
    }

    @Override
    public void clearCart(Long userId,String guestId) {
//        String redisKey = CART_KEY_PREFIX + userId;
        String redisKey = getCartRedisKey(userId, guestId);
        // 直接删除属于该用户的 Redis Key
        redisTemplate.delete(redisKey);
    }

    @Override
    public void mergeCartAfterLogin(Long userId, String guestId) {
        // 如果没有游客ID，说明不是游客登录，无需合并
        if (guestId == null || guestId.isEmpty()) return;

        String guestKey = "cart:guest:" + guestId;
        String userKey = "cart:" + userId;

        // 获取游客购物车所有商品
        Map<Object, Object> guestCart = redisTemplate.opsForHash().entries(guestKey);

        if (guestCart == null || guestCart.isEmpty()) return;

        // 遍历合并
        for (Object skuId : guestCart.keySet()) {
            RedisCartItem guestItem = (RedisCartItem) guestCart.get(skuId);

            // 查询用户购物车是否已有该 SKU
            RedisCartItem userItem = (RedisCartItem) redisTemplate.opsForHash().get(userKey, skuId);

            if (userItem != null) {
                // 已有 → 累加数量，勾选状态取最大（只要有选中就选中）
                int qty = userItem.getQuantity() + guestItem.getQuantity();
                int checked = Math.max(userItem.getChecked(), guestItem.getChecked());

                redisTemplate.opsForHash().put(userKey, skuId, new RedisCartItem(qty, checked));
            } else {
                // 没有 → 直接写入用户购物车
                redisTemplate.opsForHash().put(userKey, skuId, guestItem);
            }
        }

        // 删除游客购物车
        redisTemplate.delete(guestKey);
    }
}
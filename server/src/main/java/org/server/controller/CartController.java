package org.server.controller;

import org.server.common.Result;
import org.server.dto.CartDTO;
import org.server.utils.BaseContext;
import org.server.vo.CartVO;
import org.server.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    @GetMapping("/list")
    public Result<CartVO> getCartList(@RequestParam(required = false) String guestId) {
        Long userId = BaseContext.getCurrentId();
        return Result.success(cartService.getCartList(userId, guestId));
    }

    @PostMapping
    public Result<Void> addToCart(@RequestBody @Validated CartDTO.CartAddDTO cartAddDTO) {
        Long userId = BaseContext.getCurrentId(); // 登录才有
        String guestId = cartAddDTO.getGuestId();        // 前端传

        cartService.addToCart(userId, guestId, cartAddDTO);
        return Result.success("添加商品成功", null);
    }

    @PutMapping("/{id}")
    public Result<Void> updateCartQuantity(
            @PathVariable("id") Long cartId,
            @RequestBody @Validated CartDTO.CartUpdateStepDTO dto
    ) {
        Long userId = BaseContext.getCurrentId();
        cartService.updateCartStep(userId, dto.getGuestId(), cartId, dto.getAction());
        return Result.success("购物车更新成功", null);
    }

    @PutMapping("/checked/{id}")
    public Result<Void> updateCartChecked(@PathVariable("id") Long cartId, @RequestBody @Validated CartDTO.CartCheckedDTO dto) {
        Long userId = BaseContext.getCurrentId();
        // 从 DTO 获取
        cartService.updateCartChecked(userId, dto.getGuestId(), cartId, dto.getChecked());
        return Result.success("状态修改成功", null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteCartItem(@PathVariable("id") Long cartId, @RequestParam(required = false) String guestId) {
        // 对于 DELETE 请求，通常没有 @RequestBody，建议使用 @RequestParam 接收 URL 参数
        Long userId = BaseContext.getCurrentId();
        cartService.deleteCartItem(userId, guestId, cartId);
        return Result.success("删除成功", null);
    }

    @DeleteMapping("/clear")
    public Result<Void> clearCart(@RequestParam(required = false) String guestId) {
        // 同样，清空接口没有 body，使用 @RequestParam
        Long userId = BaseContext.getCurrentId();
        cartService.clearCart(userId, guestId);
        return Result.success("购物车清空成功", null);
    }
}
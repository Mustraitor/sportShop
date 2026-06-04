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
    public Result<Void> updateCartQuantity(@PathVariable("id") Long cartId, String guestId, @RequestBody @Validated CartDTO.CartUpdateStepDTO dto) {
        Long userId = BaseContext.getCurrentId();
        cartService.updateCartStep(userId, guestId, cartId, dto.getAction());
        return Result.success("购物车更新成功", null);
    }

    @PutMapping("/checked/{id}")
    public Result<Void> updateCartChecked(@PathVariable("id") Long cartId,String guestId, @RequestBody @Validated CartDTO.CartCheckedDTO dto) {
        Long userId = BaseContext.getCurrentId();
        cartService.updateCartChecked(userId,guestId,cartId, dto.getChecked());
        return Result.success("状态修改成功", null);
    }

    // 🌟 补上删除单项的接口，这样 DELETE /cart/1 就能跑通了
    @DeleteMapping("/{id}")
    public Result<Void> deleteCartItem(@PathVariable("id") Long cartId, String guestId) {
        Long userId = BaseContext.getCurrentId();
        cartService.deleteCartItem(userId,guestId,cartId);
        return Result.success("删除成功", null);
    }

    @DeleteMapping("/clear")
    public Result<Void> clearCart(String guestId) {
        Long userId = BaseContext.getCurrentId();
        cartService.clearCart(userId, guestId);
        return Result.success("购物车清空成功", null);
    }
}
package com.me.controller.user;

import com.me.dto.CartItemDTO;
import com.me.result.Result;
import com.me.service.CartService;
import com.me.utils.JwtUtil;
import com.me.vo.CartItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final JwtUtil jwtUtil;

    // 获取购物车列表
    @GetMapping("/list")
    public Result<List<CartItemVO>> getCartList(@RequestHeader("Authorization") String authHeader) {
        Long userId = jwtUtil.getUserIdFromAuthHeader(authHeader);
        List<CartItemVO> list = cartService.getCartItemList(userId);
        return Result.success(list);
    }

    // 加入购物车
    @PostMapping("/add")
    public Result addToCart(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody CartItemDTO dto
    ) {
        Long userId = jwtUtil.getUserIdFromAuthHeader(authHeader);
        try {
            cartService.addItem(userId, dto);
            return Result.success();
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    // 修改购物车商品数量
    @PostMapping("/update")
    public Result updateCartItem(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> data
    ) {
        Long userId = jwtUtil.getUserIdFromAuthHeader(authHeader);
        Long productId = Long.valueOf(data.get("productId").toString());
        Integer quantity = Integer.valueOf(data.get("quantity").toString());

        try {
            cartService.updateItemQuantity(userId, productId, quantity);
            return Result.success();
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    // 删除购物车商品
    @PostMapping("/delete")
    public Result deleteCartItem(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam Long productId
    ) {
        Long userId = jwtUtil.getUserIdFromAuthHeader(authHeader);
        cartService.deleteItem(userId, productId);
        return Result.success();
    }

    // 清空购物车
    @PostMapping("/clear")
    public Result clearCart(@RequestHeader("Authorization") String authHeader) {
        Long userId = jwtUtil.getUserIdFromAuthHeader(authHeader);
        cartService.clearCart(userId);
        return Result.success();
    }
}
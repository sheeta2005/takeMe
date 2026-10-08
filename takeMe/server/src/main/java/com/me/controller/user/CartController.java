package com.me.controller.user;

import com.me.dto.CartItemDTO;
import com.me.result.Result;
import com.me.service.CartService;
import com.me.utils.JwtUtil;
import com.me.vo.CartItemVO;
import com.me.vo.OrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "用户-购物车管理")
@RestController
@RequestMapping("/api/user/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final JwtUtil jwtUtil;

    //查询购物车服务
    @Operation(summary = "列表查")
    @GetMapping("/list")
    public Result<List<CartItemVO>> getCartList(@RequestHeader("Authorization") String authHeader) {
        Long userId = jwtUtil.getUserIdFromAuthHeader(authHeader);
        List<CartItemVO> list = cartService.getCartItemList(userId);
        return Result.success(list);
    }

    //添加服务到购物车
    @Operation(summary = "增")
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

    //修改购物车数量
    @Operation(summary = "改")
    @PostMapping("/update")
    public Result updateCartItem(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> data
    ) {
        Long userId = jwtUtil.getUserIdFromAuthHeader(authHeader);
        Long cartItemId = Long.valueOf(data.get("cartItemId").toString());
        Integer quantity = Integer.valueOf(data.get("quantity").toString());

        try {
            cartService.updateItemQuantity(userId, cartItemId, quantity);
            return Result.success();
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    //删除购物车服务
    @Operation(summary = "删")
    @PostMapping("/delete")
    public Result deleteCartItem(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> data
    ) {
        Long userId = jwtUtil.getUserIdFromAuthHeader(authHeader);
        Long cartItemId = Long.valueOf(data.get("cartItemId").toString());
        cartService.deleteItem(userId, cartItemId);
        return Result.success();
    }

    //清空购物车
    @Operation(summary = "删所有")
    @PostMapping("/clear")
    public Result clearCart(@RequestHeader("Authorization") String authHeader) {
        Long userId = jwtUtil.getUserIdFromAuthHeader(authHeader);
        cartService.clearCart(userId);
        return Result.success();
    }

    //结算购物车
    @Operation(summary = "购买")
    @PostMapping("/checkout")
    public Result<OrderVO> checkout(@RequestHeader("Authorization") String authHeader,
                                    @RequestHeader("Idempotency-Key") String requestId) {
        Long userId = jwtUtil.getUserIdFromAuthHeader(authHeader);
        
        try {
            OrderVO orderVO = cartService.checkout(userId, requestId);
            return Result.success(orderVO);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }
}

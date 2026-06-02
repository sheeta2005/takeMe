package com.me.controller.user;

import com.me.dto.CartItemDTO;
import com.me.result.Result;
import com.me.service.CartService;
import com.me.utils.JwtUtil;
import com.me.vo.CartItemVO;
import com.me.vo.OrderVO;
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

    @GetMapping("/list")
    public Result<List<CartItemVO>> getCartList(@RequestHeader("Authorization") String authHeader) {
        Long userId = jwtUtil.getUserIdFromAuthHeader(authHeader);
        List<CartItemVO> list = cartService.getCartItemList(userId);
        return Result.success(list);
    }

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

    @PostMapping("/clear")
    public Result clearCart(@RequestHeader("Authorization") String authHeader) {
        Long userId = jwtUtil.getUserIdFromAuthHeader(authHeader);
        cartService.clearCart(userId);
        return Result.success();
    }

    @PostMapping("/checkout")
    public Result<OrderVO> checkout(@RequestHeader("Authorization") String authHeader) {
        Long userId = jwtUtil.getUserIdFromAuthHeader(authHeader);
        
        try {
            OrderVO orderVO = cartService.checkout(userId);
            return Result.success(orderVO);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }
}
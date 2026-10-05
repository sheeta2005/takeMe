package com.me.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.me.dto.CartItemDTO;
import com.me.dto.OrderDTO;
import com.me.dto.OrderItemDTO;
import com.me.entity.Cart;
import com.me.entity.CartItem;
import com.me.entity.ServicePackage;
import com.me.exception.ShoppingCartBusinessException;
import com.me.mapper.CartItemMapper;
import com.me.mapper.CartMapper;
import com.me.mapper.ServicePackageMapper;
import com.me.mapper.UserMapper;
import com.me.mapper.OrderMapper;
import com.me.entity.Order;
import com.me.service.CartService;
import com.me.service.OrderService;
import com.me.vo.CartItemVO;
import com.me.vo.OrderVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartServiceImpl extends ServiceImpl<CartMapper, Cart> implements CartService {

    private final CartItemMapper cartItemMapper;
    private final OrderService orderService;
    private final ServicePackageMapper servicePackageMapper;
    private final UserMapper userMapper;
    private final OrderMapper orderMapper;

    private static final int SERVICE_TYPE_MEAL = 2;

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public List<CartItemVO> getCartItemList(Long userId) {
        Cart cart = getOrCreateCart(userId);
        LambdaQueryWrapper<CartItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CartItem::getCartId, cart.getId());

        List<CartItem> itemList = cartItemMapper.selectList(wrapper);

        return itemList.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void addItem(Long userId, CartItemDTO dto) {
        ServicePackage service = dto.getServiceId() == null ? null : servicePackageMapper.selectById(dto.getServiceId());
        if (service == null || service.getStatus() == null || service.getStatus() != 1
                || service.getPrice() == null || service.getPrice() < 0
                || dto.getQuantity() == null || dto.getQuantity() < 1
                || (service.getType() != null && service.getType() != SERVICE_TYPE_MEAL && dto.getQuantity() != 1)) {
            throw new IllegalArgumentException("服务已下架或数量无效");
        }
        dto.setServiceName(service.getName());
        dto.setServicePrice(service.getPrice());
        dto.setServiceType(service.getType());
        Cart cart = getOrCreateCart(userId);

        CartItem item = convertToEntity(dto);
        item.setCartId(cart.getId());

        LambdaQueryWrapper<CartItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CartItem::getCartId, cart.getId())
                .eq(CartItem::getServiceId, item.getServiceId());
        
        if (item.getServiceDate() != null && !item.getServiceDate().isEmpty()) {
            wrapper.eq(CartItem::getServiceDate, item.getServiceDate());
        }
        if (item.getServiceTime() != null && !item.getServiceTime().isEmpty()) {
            wrapper.eq(CartItem::getServiceTime, item.getServiceTime());
        }
        
        CartItem existingItem = cartItemMapper.selectOne(wrapper);

        if (existingItem != null) {
            if (existingItem.getServiceType() == SERVICE_TYPE_MEAL) {
                existingItem.setQuantity(existingItem.getQuantity() + item.getQuantity());
                cartItemMapper.updateById(existingItem);
            } else {
                throw new ShoppingCartBusinessException("该时间段的服务已存在，请选择其他时间");
            }
        } else {
            if (item.getServiceType() != SERVICE_TYPE_MEAL) {
                item.setQuantity(1);
            }
            item.setSelected(1);
            item.setCreateTime(LocalDateTime.now());
            cartItemMapper.insert(item);
        }

        cart.setUpdateTime(LocalDateTime.now());
        this.updateById(cart);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void updateItemQuantity(Long userId, Long productId, Integer quantity) {
        Cart cart = getOrCreateCart(userId);

        LambdaQueryWrapper<CartItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CartItem::getCartId, cart.getId())
                .eq(CartItem::getId, productId);
        CartItem item = cartItemMapper.selectOne(wrapper);

        if (item == null) {
            throw new ShoppingCartBusinessException("购物车商品不存在");
        }

        if (item.getServiceType() != SERVICE_TYPE_MEAL) {
            throw new ShoppingCartBusinessException("该服务只能预约1次，无法修改数量");
        }

        if (quantity < 1) {
            throw new IllegalArgumentException("数量必须大于零");
        }

        item.setQuantity(quantity);
        cartItemMapper.updateById(item);

        cart.setUpdateTime(LocalDateTime.now());
        this.updateById(cart);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void deleteItem(Long userId, Long productId) {
        Cart cart = getOrCreateCart(userId);

        LambdaQueryWrapper<CartItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CartItem::getCartId, cart.getId())
                .eq(CartItem::getId, productId);
        cartItemMapper.delete(wrapper);

        cart.setUpdateTime(LocalDateTime.now());
        this.updateById(cart);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void clearCart(Long userId) {
        Cart cart = getOrCreateCart(userId);

        LambdaQueryWrapper<CartItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CartItem::getCartId, cart.getId());
        cartItemMapper.delete(wrapper);

        cart.setUpdateTime(LocalDateTime.now());
        this.updateById(cart);
    }
    
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public OrderVO checkout(Long userId, String requestId) {
        if (requestId == null || !requestId.matches("[A-Za-z0-9_-]{1,64}")) {
            throw new IllegalArgumentException("提交标识缺失或格式错误");
        }
        if (userMapper.selectForUpdate(userId) == null) throw new IllegalArgumentException("账号不存在");
        // 重试先返回已建订单，不再读取空购物车，也不清空随后新加入的服务。
        Order existing = orderMapper.selectByRequestForUpdate(userId, requestId);
        if (existing != null) return orderService.getOrderDetail(userId, existing.getId());
        Cart cart = getOrCreateCart(userId);
        
        LambdaQueryWrapper<CartItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CartItem::getCartId, cart.getId());
        List<CartItem> cartItems = cartItemMapper.selectList(wrapper);
        
        if (cartItems == null || cartItems.isEmpty()) {
            throw new ShoppingCartBusinessException("购物车为空");
        }
        
        List<OrderItemDTO> itemDTOList = cartItems.stream().map(item -> {
            OrderItemDTO dto = new OrderItemDTO();
            dto.setServiceId(item.getServiceId());
            dto.setServiceName(item.getServiceName());
            dto.setServicePrice(item.getServicePrice());
            dto.setServiceType(item.getServiceType());
            dto.setQuantity(item.getQuantity());
            dto.setServiceDate(item.getServiceDate());
            dto.setServiceTime(item.getServiceTime());
            dto.setAddress(item.getAddress());
            dto.setRemark(item.getRemark());
            
            dto.setItemPrice(item.getServicePrice() * item.getQuantity());
            
            return dto;
        }).collect(Collectors.toList());
        
        OrderDTO orderDTO = new OrderDTO();
        orderDTO.setRequestId(requestId);
        
        OrderVO orderVO = orderService.createOrder(userId, orderDTO, itemDTOList);
        
        clearCart(userId);
        
        return orderVO;
    }

    private Cart getOrCreateCart(Long userId) {
        // 与结算共用账号行锁，防止两个标签页重复建购物车或覆盖结算中的内容。
        if (userMapper.selectForUpdate(userId) == null) throw new IllegalArgumentException("账号不存在");
        LambdaQueryWrapper<Cart> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cart::getUserId, userId);
        Cart cart = this.getOne(wrapper);

        if (cart == null) {
            cart = new Cart();
            cart.setUserId(userId);
            cart.setCreateTime(LocalDateTime.now());
            cart.setUpdateTime(LocalDateTime.now());
            this.save(cart);
        }

        return cart;
    }

    private CartItem convertToEntity(CartItemDTO dto) {
        CartItem item = new CartItem();
        BeanUtils.copyProperties(dto, item);
        return item;
    }

    private CartItemVO convertToVO(CartItem item) {
        CartItemVO vo = new CartItemVO();
        BeanUtils.copyProperties(item, vo);
        return vo;
    }
}

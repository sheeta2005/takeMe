package com.me.service.Impl;

import com.me.dto.OrderDTO;
import com.me.dto.OrderItemDTO;
import com.me.dto.PageResultDTO;
import com.me.entity.User;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.me.entity.Order;
import com.me.entity.OrderItem;
import com.me.entity.PaymentTransaction;
import com.me.entity.ServicePackage;
import com.me.mapper.OrderItemMapper;
import com.me.mapper.OrderMapper;
import com.me.mapper.ServicePackageMapper;
import com.me.redis.annotation.RedisCache;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderP0Test {

    @BeforeAll
    static void initTableMetadata() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new Configuration(), ""), OrderItem.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new Configuration(), ""), Order.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new Configuration(), ""), User.class);
    }

    @Mock private OrderMapper orderMapper;
    @Mock private OrderItemMapper orderItemMapper;
    @Mock private ServicePackageMapper servicePackageMapper;
    @Mock private com.me.mapper.UserMapper userMapper;
    @Mock private com.me.mapper.ReviewMapper reviewMapper;
    @Mock private com.me.mapper.VolunteerMapper volunteerMapper;
    @Mock private com.me.mapper.VolunteerPointsRecordMapper pointsRecordMapper;
    @Mock private com.me.service.OutboxService outboxService;
    @Mock private com.me.service.MessageService messageService;
    @Mock private com.me.redis.utils.RedisUtil redisUtil;
    @Mock private com.me.mapper.PaymentTransactionMapper paymentTransactionMapper;
    @InjectMocks private OrderServiceImpl orderService;

    @Test
    void elderPageFetchesItemsOnceNotOncePerOrder() {
        Order first = new Order();
        first.setId(10L);
        Order second = new Order();
        second.setId(20L);
        Page<Order> page = new Page<>(1, 10, 2);
        page.setRecords(List.of(first, second));
        when(orderMapper.selectPage(any(Page.class), any())).thenReturn(page);
        OrderItem item = new OrderItem();
        item.setOrderId(20L);
        when(orderItemMapper.selectList(any())).thenReturn(List.of(item));
        var result = orderService.getMyOrderList(1L, null, null, new PageResultDTO());
        assertTrue(result.getRecords().get(0).getItems().isEmpty());
        assertEquals(1, result.getRecords().get(1).getItems().size());
        verify(orderItemMapper, times(1)).selectList(any());
    }

    @Test
    void availablePageBatchesParentsAndUsersWithoutChangingItemSemantics() {
        OrderItem first = new OrderItem();
        first.setId(11L);
        first.setOrderId(10L);
        OrderItem second = new OrderItem();
        second.setId(12L);
        second.setOrderId(10L);
        Page<OrderItem> page = new Page<>(1, 10, 2);
        page.setRecords(List.of(first, second));
        when(orderItemMapper.selectPage(any(Page.class), any())).thenReturn(page);
        Order order = new Order();
        order.setId(10L);
        order.setUserId(1L);
        order.setStatus(0);
        User user = new User();
        user.setId(1L);
        user.setRealName("测试老人");
        when(orderMapper.selectBatchIds(anyCollection())).thenReturn(List.of(order));
        when(userMapper.selectBatchIds(anyCollection())).thenReturn(List.of(user));
        var result = orderService.getAvailableOrderList(new PageResultDTO());
        assertEquals(2, result.getRecords().size());
        assertEquals(12L, result.getRecords().get(1).getItems().get(0).getId());
        assertEquals("测试老人", result.getRecords().get(0).getUserName());
        verify(orderMapper).selectBatchIds(List.of(10L));
        verify(userMapper).selectBatchIds(List.of(1L));
        verify(orderMapper, never()).selectById(anyLong());
        verify(userMapper, never()).selectById(anyLong());
    }

    @Test
    void emptyPageDoesNotQueryUnboundedBatch() {
        when(orderItemMapper.selectPage(any(Page.class), any())).thenReturn(new Page<OrderItem>());
        assertTrue(orderService.getAvailableOrderList(new PageResultDTO()).getRecords().isEmpty());
        verifyNoInteractions(orderMapper, userMapper);
    }

    @Test
    void orderDetailMustCheckOwnershipOnEveryRequest() throws Exception {
        assertNull(OrderServiceImpl.class.getMethod("getOrderDetail", Long.class, Long.class)
                .getAnnotation(RedisCache.class));
        Order order = new Order();
        order.setId(10L);
        order.setUserId(1L);
        when(orderMapper.selectById(10L)).thenReturn(order);
        assertThrows(RuntimeException.class, () -> orderService.getOrderDetail(2L, 10L));
        verifyNoInteractions(orderItemMapper);
    }

    @Test
    void orderPriceComesFromServicePackage() {
        ServicePackage service = new ServicePackage();
        service.setId(4L);
        service.setName("助餐");
        service.setType(2);
        service.setPrice(100);
        service.setStatus(1);
        when(servicePackageMapper.selectById(4L)).thenReturn(service);
        when(orderMapper.insert(any(Order.class))).thenAnswer(invocation -> {
            invocation.<Order>getArgument(0).setId(10L);
            return 1;
        });
        when(orderItemMapper.selectList(any())).thenReturn(List.of());

        OrderItemDTO item = new OrderItemDTO();
        item.setServiceId(4L);
        item.setServicePrice(1);
        item.setItemPrice(2);
        item.setQuantity(2);
        item.setServiceDate(LocalDate.now().plusDays(2).toString());
        item.setServiceTime("10:00");
        item.setAddress("测试地址");
        OrderDTO dto = submission();
        orderService.createOrder(1L, dto, List.of(item));

        var saved = org.mockito.ArgumentCaptor.forClass(Order.class);
        verify(orderMapper).insert(saved.capture());
        assertEquals(200, saved.getValue().getTotalPrice());
    }

    @Test
    void cancellingOrderCancelsPendingAndAssignedItems() {
        Order order = new Order();
        order.setId(10L);
        order.setUserId(1L);
        order.setStatus(1);
        OrderItem pending = new OrderItem();
        pending.setId(11L);
        pending.setOrderId(10L);
        pending.setItemStatus(0);
        pending.setItemPrice(100);
        OrderItem assigned = new OrderItem();
        assigned.setId(12L);
        assigned.setOrderId(10L);
        assigned.setItemStatus(1);
        assigned.setVolunteerId(2L);
        assigned.setItemPrice(100);
        PaymentTransaction paid = new PaymentTransaction();
        paid.setPaymentStatus(1);
        paid.setPaymentMethod("mock");
        when(orderMapper.selectForUpdate(10L)).thenReturn(order);
        when(paymentTransactionMapper.selectList(any())).thenReturn(List.of(paid));
        when(paymentTransactionMapper.selectCount(any())).thenReturn(0L);
        when(orderItemMapper.selectByOrderForUpdate(10L)).thenReturn(List.of(pending, assigned));
        when(orderItemMapper.clearVolunteer(anyLong(), anyInt(), anyInt(), nullable(Long.class))).thenReturn(1);
        when(orderMapper.update(isNull(), any())).thenReturn(1);

        orderService.cancelOrder(1L, 10L);

        assertEquals(5, pending.getItemStatus());
        assertEquals(5, assigned.getItemStatus());
        assertEquals(5, order.getStatus());
        assertEquals(2, paid.getPaymentStatus());
        verify(paymentTransactionMapper, times(2)).insert(any(PaymentTransaction.class));
    }

    @Test
    void expiryOnlyCancelsTheDueItemAndRecordsItsMockRefundOnce() {
        Order order = new Order();
        order.setId(10L);
        order.setOrderNo("ORD10");
        order.setUserId(1L);
        order.setStatus(0);
        OrderItem due = new OrderItem();
        due.setId(11L);
        due.setOrderId(10L);
        due.setItemStatus(0);
        due.setItemPrice(100);
        due.setServiceDate(LocalDate.now().minusDays(1).toString());
        due.setServiceTime("09:00");
        OrderItem future = new OrderItem();
        future.setId(12L);
        future.setOrderId(10L);
        future.setItemStatus(0);
        future.setServiceDate(LocalDate.now().plusDays(1).toString());
        future.setServiceTime("10:00");
        PaymentTransaction paid = new PaymentTransaction();
        paid.setPaymentStatus(1);
        paid.setPaymentMethod("mock");
        paid.setPaymentTime(LocalDateTime.now().minusDays(1));
        when(orderItemMapper.selectForUpdate(11L)).thenReturn(due);
        when(orderMapper.selectByItemForUpdate(11L)).thenReturn(order);
        when(orderMapper.selectForUpdate(10L)).thenReturn(order);
        when(orderMapper.selectById(10L)).thenReturn(order);
        when(orderItemMapper.selectByOrderForUpdate(10L)).thenReturn(List.of(due, future));
        when(orderItemMapper.clearVolunteer(11L, 0, 5, null)).thenReturn(1);
        when(orderMapper.update(isNull(), any())).thenReturn(1);
        when(paymentTransactionMapper.selectList(any())).thenReturn(List.of(paid));
        when(paymentTransactionMapper.selectCount(any())).thenReturn(0L);

        orderService.expirePendingItem(11L);
        orderService.expirePendingItem(11L);

        assertEquals(5, due.getItemStatus());
        assertEquals(0, future.getItemStatus());
        assertEquals(0, order.getStatus());
        var refund = org.mockito.ArgumentCaptor.forClass(PaymentTransaction.class);
        verify(paymentTransactionMapper).insert(refund.capture());
        assertEquals(100, refund.getValue().getAmount());
        assertEquals("REF11", refund.getValue().getTransactionNo());
        assertEquals(1, paid.getPaymentStatus());
    }

    @Test
    void cancelledParentCannotBeClaimedAgain() {
        OrderItem item = new OrderItem();
        item.setId(11L);
        item.setOrderId(10L);
        item.setItemStatus(0);
        Order order = new Order();
        order.setId(10L);
        order.setStatus(5);
        com.me.entity.Volunteer volunteer = new com.me.entity.Volunteer();
        volunteer.setStatus(1);
        volunteer.setPoints(100);
        when(volunteerMapper.selectForUpdate(2L)).thenReturn(volunteer);
        when(orderItemMapper.selectActiveForVolunteer(2L)).thenReturn(List.of());
        when(orderMapper.selectByItemForUpdate(11L)).thenReturn(order);

        assertThrows(RuntimeException.class, () -> orderService.volunteerConfirmOrder(2L, 11L));
        verify(orderItemMapper, never()).update(any(), any());
    }

    @Test
    void unassignedVolunteerCannotReadOrderDetail() {
        Order order = new Order();
        order.setId(10L);
        when(orderMapper.selectById(10L)).thenReturn(order);
        when(orderItemMapper.selectCount(any())).thenReturn(0L);
        assertThrows(RuntimeException.class, () -> orderService.getVolunteerOrderDetail(2L, 10L));
        verify(orderItemMapper, never()).selectList(any());
    }

    @Test
    void orderMustLeaveAnHourForVolunteerToAccept() {
        ServicePackage service = new ServicePackage();
        service.setId(4L);
        service.setType(2);
        service.setPrice(100);
        service.setStatus(1);
        when(servicePackageMapper.selectById(4L)).thenReturn(service);
        LocalDateTime soon = LocalDateTime.now().plusMinutes(30);
        OrderItemDTO item = new OrderItemDTO();
        item.setServiceId(4L);
        item.setQuantity(1);
        item.setServiceDate(soon.toLocalDate().toString());
        item.setServiceTime(soon.format(DateTimeFormatter.ofPattern("HH:mm")));
        item.setAddress("测试地址");
        assertThrows(IllegalArgumentException.class,
                () -> orderService.createOrder(1L, submission(), List.of(item)));
        verify(orderMapper, never()).insert(any(Order.class));
    }

    private OrderDTO submission() {
        com.me.entity.User user = new com.me.entity.User();
        user.setId(1L);
        user.setStatus(1);
        when(userMapper.selectForUpdate(1L)).thenReturn(user);
        OrderDTO dto = new OrderDTO();
        dto.setRequestId("test-submit");
        return dto;
    }

    @Test
    void reviewRatingMustBeOneToFive() {
        assertThrows(IllegalArgumentException.class, () -> orderService.evaluateOrderItem(1L, 1L, 0, ""));
        assertThrows(IllegalArgumentException.class, () -> orderService.evaluateOrderItem(1L, 1L, 6, ""));
        verifyNoInteractions(orderMapper, orderItemMapper, reviewMapper);
    }
}

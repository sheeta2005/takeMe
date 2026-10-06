package com.me.mq;

import com.me.dto.OrderTimeoutMessage;
import com.me.mq.consumer.OrderTimeoutConsumer;
import com.me.service.OrderService;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderTimeoutP0Test {

    @Test
    void duplicateDeliveryIsRecheckedAndAcknowledgementIsOwnedByContainer() throws Exception {
        OrderService service = mock(OrderService.class);
        Channel channel = mock(Channel.class);
        Message message = new Message(new byte[0]);
        message.getMessageProperties().setDeliveryTag(42L);
        OrderTimeoutConsumer consumer = new OrderTimeoutConsumer(service);
        OrderTimeoutMessage timeout = new OrderTimeoutMessage(10L, "ORD10", 1L);

        consumer.handleOrderTimeout(timeout, message, channel);
        consumer.handleOrderTimeout(timeout, message, channel);

        verify(service, times(2)).expirePendingItems(10L);
        verifyNoInteractions(channel);
    }

    @Test
    void failedHandlingPropagatesToBoundedContainerRetry() throws Exception {
        OrderService service = mock(OrderService.class);
        Channel channel = mock(Channel.class);
        Message message = new Message(new byte[0]);
        message.getMessageProperties().setDeliveryTag(42L);
        doThrow(new IllegalStateException("database unavailable")).when(service).expirePendingItems(10L);

        assertThrows(IllegalStateException.class, () -> new OrderTimeoutConsumer(service).handleOrderTimeout(
                new OrderTimeoutMessage(10L, "ORD10", 1L), message, channel));
        verifyNoInteractions(channel);
    }
}

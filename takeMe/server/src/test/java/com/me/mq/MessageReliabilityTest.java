package com.me.mq;

import com.me.mq.config.RabbitMQConfig;
import com.me.mq.producer.MessageProducer;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MessageReliabilityTest {

    @Test
    void confirmedSendPreservesEventIdAndPersistence() {
        RabbitTemplate template = mock(RabbitTemplate.class);
        MessageProducer producer = new MessageProducer(template);
        ReflectionTestUtils.setField(producer, "rabbitmqEnabled", true);
        doAnswer(call -> {
            Message raw = new Message(new byte[0]);
            Message processed = call.<MessagePostProcessor>getArgument(3).postProcessMessage(raw);
            assertEquals("event-1", processed.getMessageProperties().getMessageId());
            assertEquals(MessageDeliveryMode.PERSISTENT, processed.getMessageProperties().getDeliveryMode());
            call.<CorrelationData>getArgument(4).getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(template).convertAndSend(anyString(), anyString(), any(Object.class),
                any(MessagePostProcessor.class), any(CorrelationData.class));
        producer.sendConfirmed("exchange", "route", "内容", "event-1");
    }

    @Test
    void returnedMessageIsFailureEvenWhenExchangeConfirmsIt() {
        RabbitTemplate template = mock(RabbitTemplate.class);
        MessageProducer producer = new MessageProducer(template);
        ReflectionTestUtils.setField(producer, "rabbitmqEnabled", true);
        doAnswer(call -> {
            CorrelationData correlation = call.getArgument(4);
            correlation.setReturned(new ReturnedMessage(new Message(new byte[0]), 312,
                    "NO_ROUTE", "exchange", "missing"));
            correlation.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(template).convertAndSend(anyString(), anyString(), any(Object.class),
                any(MessagePostProcessor.class), any(CorrelationData.class));
        assertThrows(AmqpException.class,
                () -> producer.sendConfirmed("exchange", "missing", "内容", "event-1"));
    }

    @Test
    void nackIsFailure() {
        RabbitTemplate template = mock(RabbitTemplate.class);
        MessageProducer producer = new MessageProducer(template);
        ReflectionTestUtils.setField(producer, "rabbitmqEnabled", true);
        doAnswer(call -> {
            call.<CorrelationData>getArgument(4).getFuture().complete(new CorrelationData.Confirm(false, "失败"));
            return null;
        }).when(template).convertAndSend(anyString(), anyString(), any(Object.class),
                any(MessagePostProcessor.class), any(CorrelationData.class));
        assertThrows(AmqpException.class, () -> producer.sendConfirmed("exchange", "route", "内容", "event-1"));
    }

    @Test
    void approvalBindingAndMandatoryAreConfigured() {
        RabbitMQConfig config = new RabbitMQConfig();
        assertEquals(RabbitMQConfig.APPROVAL_RESULT_ROUTING_KEY,
                config.volunteerApprovalBinding(new Queue("test"), config.approvalResultDirectExchange())
                        .getRoutingKey());
        CachingConnectionFactory connection = new CachingConnectionFactory();
        try {
            RabbitTemplate template = config.rabbitTemplate(connection);
            assertTrue(template.isMandatoryFor(new Message(new byte[0])));
            assertTrue(connection.isPublisherConfirms());
            assertTrue(connection.isPublisherReturns());
        } finally {
            connection.destroy();
        }
    }
}

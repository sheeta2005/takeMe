package com.me.mq;

import com.me.mq.aspect.MqLogAspect;
import com.me.mq.producer.MessageProducer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(OutputCaptureExtension.class)
class MqLogAspectTest {

    private MessageProducer proxy(RabbitTemplate template, boolean enabled) {
        MessageProducer producer = new MessageProducer(template);
        ReflectionTestUtils.setField(producer, "rabbitmqEnabled", enabled);
        MqLogAspect aspect = new MqLogAspect();
        ReflectionTestUtils.setField(aspect, "rabbitmqEnabled", enabled);
        AspectJProxyFactory factory = new AspectJProxyFactory(producer);
        factory.addAspect(aspect);
        return factory.getProxy();
    }

    @Test
    void bothSendingPathsLogExactlyOnceAndNeverLogPayload(CapturedOutput output) {
        RabbitTemplate template = mock(RabbitTemplate.class);
        doAnswer(call -> {
            call.<CorrelationData>getArgument(4).getFuture()
                    .complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(template).convertAndSend(anyString(), anyString(), any(Object.class),
                any(MessagePostProcessor.class), any(CorrelationData.class));
        MessageProducer producer = proxy(template, true);
        producer.sendMessage("exchange", "route", "测试消息内容");
        producer.sendConfirmed("exchange", "route", "测试消息内容", "event-1");
        assertThat(output.getAll().split("MQ消息发送成功", -1)).hasSize(3);
        assertThat(output.getAll()).contains("event-1").doesNotContain("测试消息内容");
    }

    @Test
    void confirmationFailureIsLoggedAndPropagated(CapturedOutput output) {
        RabbitTemplate template = mock(RabbitTemplate.class);
        doAnswer(call -> {
            call.<CorrelationData>getArgument(4).getFuture()
                    .complete(new CorrelationData.Confirm(false, "测试拒绝"));
            return null;
        }).when(template).convertAndSend(anyString(), anyString(), any(Object.class),
                any(MessagePostProcessor.class), any(CorrelationData.class));
        assertThrows(AmqpException.class,
                () -> proxy(template, true).sendConfirmed("exchange", "route", "内容", "event-2"));
        assertThat(output.getAll()).contains("MQ消息发送失败", "event-2")
                .doesNotContain("MQ消息发送成功");
    }

    @Test
    void disabledMqDoesNotReportDeliverySuccess(CapturedOutput output) {
        RabbitTemplate template = mock(RabbitTemplate.class);
        proxy(template, false).sendMessage("exchange", "route", "内容");
        verifyNoInteractions(template);
        assertThat(output.getAll()).doesNotContain("MQ消息发送成功");
    }
}

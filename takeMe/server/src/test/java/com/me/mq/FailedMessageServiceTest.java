package com.me.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.me.entity.FailedMessage;
import com.me.mapper.FailedMessageMapper;
import com.me.mq.config.RabbitMQConfig;
import com.me.mq.producer.MessageProducer;
import com.me.service.FailedMessageService;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FailedMessageServiceTest {
    @Test
    void archivesRawPoisonMessageWithItsOriginalConsumerQueue() {
        FailedMessageMapper mapper = mock(FailedMessageMapper.class);
        when(mapper.archive(any(FailedMessage.class))).thenReturn(1);
        FailedMessageService service = new FailedMessageService(mapper, new ObjectMapper(), mock(MessageProducer.class));
        MessageProperties properties = new MessageProperties();
        properties.setMessageId("event-1");
        properties.setConsumerQueue("notification.user.queue");
        properties.getHeaders().put(RabbitMQConfig.FAILED_SOURCE_QUEUE_HEADER, "notification.user.queue");
        properties.getHeaders().put("x-exception-message", "毒消息无法解析");
        Message malformed = new Message(new byte[]{0, (byte) 0xff, 1}, properties);

        service.archive(malformed);

        var captor = org.mockito.ArgumentCaptor.forClass(FailedMessage.class);
        verify(mapper).archive(captor.capture());
        assertArrayEquals(malformed.getBody(), captor.getValue().getBody());
        assertEquals("event-1", captor.getValue().getMessageId());
        assertEquals("notification.user.queue", captor.getValue().getSourceQueue());
        assertEquals("consumer-failure", captor.getValue().getReason());
        assertTrue(captor.getValue().getHeadersJson().contains("毒消息无法解析"));
    }

    @Test
    void unknownQueueOverflowRemainsArchivedButCannotBeReplayed() {
        FailedMessageMapper mapper = mock(FailedMessageMapper.class);
        FailedMessage archived = new FailedMessage();
        archived.setId("unknown-id");
        archived.setMessageId("event-1");
        archived.setSourceQueue("");
        archived.setStatus(0);
        archived.setReplayCount(0);
        when(mapper.selectForReplay("unknown-id")).thenReturn(archived);
        when(mapper.recordReplay(any(FailedMessage.class))).thenReturn(1);
        MessageProducer producer = mock(MessageProducer.class);
        FailedMessageService service = new FailedMessageService(mapper, new ObjectMapper(), producer);

        assertFalse(service.replay("unknown-id", 25L));

        verify(mapper).recordReplay(archived);
        assertEquals("来源队列未知或不允许重放", archived.getLastReplayError());
        verifyNoInteractions(producer);
    }
}

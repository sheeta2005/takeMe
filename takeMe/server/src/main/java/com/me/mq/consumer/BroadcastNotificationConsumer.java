package com.me.mq.consumer;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.me.entity.Message;
import com.me.entity.User;
import com.me.entity.Volunteer;
import com.me.mapper.UserMapper;
import com.me.mapper.VolunteerMapper;
import com.me.mq.config.RabbitMQConfig;
import com.me.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnExpression("${middleware.enabled:true} && ${middleware.rabbitmq.enabled:true}")
@RequiredArgsConstructor
public class BroadcastNotificationConsumer {
    private final UserMapper userMapper;
    private final VolunteerMapper volunteerMapper;
    private final MessageService messageService;

    @RabbitListener(queues = RabbitMQConfig.BROADCAST_QUEUE, containerFactory = "reliableRabbitListenerContainerFactory")
    public void handle(Message template, org.springframework.amqp.core.Message raw) {
        if (template.getReceiverType() == null
                || (template.getReceiverType() != 1 && template.getReceiverType() != 2)) {
            throw new IllegalArgumentException("群发接收者类型无效");
        }
        long lastId = 0;
        while (true) {
            // 按主键续页，不因前面账号停用而漏掉后面的接收者。
            List<Long> ids = template.getReceiverType() == 2
                    ? userMapper.selectList(new LambdaQueryWrapper<User>().select(User::getId)
                        .eq(User::getStatus, 1).gt(User::getId, lastId).orderByAsc(User::getId)
                        .last("LIMIT 500")).stream().map(User::getId).toList()
                    : volunteerMapper.selectList(new LambdaQueryWrapper<Volunteer>().select(Volunteer::getId)
                        .eq(Volunteer::getStatus, 1).gt(Volunteer::getId, lastId).orderByAsc(Volunteer::getId)
                        .last("LIMIT 500")).stream().map(Volunteer::getId).toList();
            if (ids.isEmpty()) return;
            for (Long id : ids) {
                Message notification = new Message();
                BeanUtils.copyProperties(template, notification);
                notification.setId(null);
                notification.setReceiverId(id);
                // 每个接收者独立提交；重投通过事件唯一键跳过已完成者。
                messageService.sendEventMessage(notification, raw.getMessageProperties().getMessageId());
                com.me.websocket.OrderWebSocketEndpoint.sendMessageReminder(template.getReceiverType(), id, notification);
            }
            lastId = ids.get(ids.size() - 1);
        }
    }
}

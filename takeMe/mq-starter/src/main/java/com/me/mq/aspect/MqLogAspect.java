package com.me.mq.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class MqLogAspect {
    //日志切面
    @Value("#{${middleware.enabled:true} && ${middleware.rabbitmq.enabled:true}}")
    private boolean rabbitmqEnabled;

    // 普通发送的内部调用不会再次经过代理；直接确认发送也需要记录结果。
    @Around("execution(* com.me.mq.producer.MessageProducer.sendMessage(..)) || " +
            "execution(* com.me.mq.producer.MessageProducer.sendConfirmed(..))")
    public Object logMqMessage(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();

        if (args.length < 3) {
            return joinPoint.proceed();
        }

        // 关闭中间件时普通发送仅跳过，不能记录为已成功投递。
        if (!rabbitmqEnabled && "sendMessage".equals(joinPoint.getSignature().getName())) {
            return joinPoint.proceed();
        }

        String exchange = (String) args[0];
        String routingKey = (String) args[1];
        Object message = args[2];

        String messageType = message != null ? message.getClass().getSimpleName() : "null";
        Object eventId = args.length == 4 ? args[3] : "-";

        long startTime = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();

            long endTime = System.currentTimeMillis();
            long costTime = endTime - startTime;

            log.info("MQ消息发送成功 | Exchange: {} | RoutingKey: {} | MessageType: {} | EventId: {} | Cost: {}ms",
                    exchange, routingKey, messageType, eventId, costTime);

            return result;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            long costTime = endTime - startTime;

            log.error("MQ消息发送失败 | Exchange: {} | RoutingKey: {} | MessageType: {} | EventId: {} | Cost: {}ms | Error: {}",
                    exchange, routingKey, messageType, eventId, costTime, e.getMessage());

            throw e;
        }
    }
}


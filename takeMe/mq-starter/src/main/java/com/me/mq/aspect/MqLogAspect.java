package com.me.mq.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class MqLogAspect {

    @Around("execution(* com.me.mq.producer.MessageProducer.sendMessage(..))")
    public Object logMqMessage(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();

        if (args.length < 3) {
            return joinPoint.proceed();
        }

        String exchange = (String) args[0];
        String routingKey = (String) args[1];
        Object message = args[2];

        String messageType = message != null ? message.getClass().getSimpleName() : "null";

        long startTime = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();

            long endTime = System.currentTimeMillis();
            long costTime = endTime - startTime;

            log.info("MQ消息发送成功 | Exchange: {} | RoutingKey: {} | MessageType: {} | Cost: {}ms",
                    exchange, routingKey, messageType, costTime);

            return result;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            long costTime = endTime - startTime;

            log.error("MQ消息发送失败 | Exchange: {} | RoutingKey: {} | MessageType: {} | Cost: {}ms | Error: {}",
                    exchange, routingKey, messageType, costTime, e.getMessage());

            throw e;
        }
    }
}


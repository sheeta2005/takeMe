package com.me.listener;

import com.me.config.MiddlewareSwitchConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MiddlewareStatusListener {

    private final MiddlewareSwitchConfig middlewareSwitchConfig;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("==================== 中间件状态 ====================");
        log.info("Redis 状态: [{}]", middlewareSwitchConfig.isRedisEnabled() ? "启用" : "禁用");
        log.info("RabbitMQ 状态: [{}]", middlewareSwitchConfig.isRabbitmqEnabled() ? "启用" : "禁用");
        log.info("==================================================");
    }
}

package com.me.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "middleware")
public class MiddlewareSwitchConfig {

    private boolean enabled = true;

    private RedisConfig redis = new RedisConfig();

    private RabbitmqConfig rabbitmq = new RabbitmqConfig();

    @Data
    public static class RedisConfig {
        private boolean enabled = true;
    }

    @Data
    public static class RabbitmqConfig {
        private boolean enabled = true;
    }

    //判断Redis是否启用
    public boolean isRedisEnabled() {
        return enabled && redis.isEnabled();
    }

    //判断RabbitMQ是否启用
    public boolean isRabbitmqEnabled() {
        return enabled && rabbitmq.isEnabled();
    }
}

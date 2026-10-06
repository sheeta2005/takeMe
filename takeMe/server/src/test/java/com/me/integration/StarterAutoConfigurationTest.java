package com.me.integration;

import com.me.mq.aspect.MqLogAspect;
import com.me.mq.config.RabbitMQConfig;
import com.me.mq.producer.MessageProducer;
import com.me.redis.aspect.RateLimitAspect;
import com.me.redis.aspect.RedisCacheAspect;
import com.me.redis.utils.RedisUtil;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.context.annotation.ImportCandidates;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class StarterAutoConfigurationTest {

    private ApplicationContextRunner runner() {
        // 从真实 imports 文件发现入口，不依赖 server 的包扫描。
        Class<?>[] candidates = StreamSupport.stream(ImportCandidates.load(AutoConfiguration.class,
                getClass().getClassLoader()).spliterator(), false)
                .filter(name -> name.startsWith("com.me.mq.") || name.startsWith("com.me.redis."))
                .map(name -> {
                    try {
                        return Class.forName(name);
                    } catch (ClassNotFoundException e) {
                        throw new AssertionError("自动配置入口不存在", e);
                    }
                }).toArray(Class<?>[]::new);
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(candidates))
                .withConfiguration(AutoConfigurations.of(AopAutoConfiguration.class,
                        RabbitAutoConfiguration.class, RedisAutoConfiguration.class))
                .withBean(CachingConnectionFactory.class, CachingConnectionFactory::new)
                .withBean(RedisConnectionFactory.class, () -> mock(RedisConnectionFactory.class));
    }

    @Test
    void startersLoadWithoutScanningTheirPackages() {
        runner().run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(MessageProducer.class)
                    .hasSingleBean(MqLogAspect.class).hasSingleBean(RabbitMQConfig.class)
                    .hasSingleBean(RedisUtil.class).hasSingleBean(RedisCacheAspect.class)
                    .hasSingleBean(RateLimitAspect.class);
            assertThat(context.getBean(RabbitTemplate.class).isMandatoryFor(
                    new org.springframework.amqp.core.Message(new byte[0]))).isTrue();
        });
    }

    @Test
    void scanningAndAutoConfigurationDoNotCreateDuplicateBeans() {
        runner().withUserConfiguration(StarterScan.class).run(context ->
                assertThat(context).hasNotFailed().hasSingleBean(MessageProducer.class)
                        .hasSingleBean(MqLogAspect.class).hasSingleBean(RedisUtil.class)
                        .hasSingleBean(RedisCacheAspect.class).hasSingleBean(RateLimitAspect.class));
    }

    @Test
    void globalSwitchDisablesCustomMqTopologyWithoutBreakingInjection() {
        runner().withPropertyValues("middleware.enabled=false").run(context ->
                assertThat(context).hasNotFailed().doesNotHaveBean(RabbitMQConfig.class)
                        .hasSingleBean(MessageProducer.class).hasSingleBean(RedisUtil.class));
    }

    @Configuration(proxyBeanMethods = false)
    @ComponentScan({"com.me.mq.config", "com.me.mq.producer", "com.me.mq.aspect", "com.me.redis"})
    static class StarterScan {
    }
}

package com.me.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PublicConfigurationTest {

    @Test
    void exampleResolvesAllPublicConfigurationWithoutLocalDevFile() throws Exception {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
        environment.getPropertySources().remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
        // 只使用无效测试凭据，验证 clone 后示例能补齐公共配置的引用。
        environment.getPropertySources().addFirst(new MapPropertySource("测试变量", Map.of(
                "TAKEME_DB_PASSWORD", "测试数据库密码",
                "TAKEME_RABBITMQ_USERNAME", "测试账号",
                "TAKEME_RABBITMQ_PASSWORD", "测试队列密码",
                "TAKEME_OSS_ENDPOINT", "oss-cn-beijing.aliyuncs.com",
                "TAKEME_OSS_BUCKET_NAME", "test-bucket",
                "TAKEME_OSS_ACCESS_KEY_ID", "test-access-id",
                "TAKEME_OSS_ACCESS_KEY_SECRET", "test-access-secret",
                "TAKEME_OSS_DOMAIN", "https://test-bucket.oss-cn-beijing.aliyuncs.com",
                "TAKEME_JWT_SECRET", "test-secret-for-configuration-validation-only")));
        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        for (String name : new String[]{"application.yml", "application-dev.example.yml", "application-community.yml"}) {
            for (var source : loader.load(name, new ClassPathResource(name))) {
                environment.getPropertySources().addFirst(source);
            }
        }
        for (var source : environment.getPropertySources()) {
            for (String key : ((EnumerablePropertySource<?>) source).getPropertyNames()) {
                assertThat(environment.getProperty(key)).as(key).isNotNull().doesNotContain("${");
            }
        }
        assertThat(environment.getProperty("me.datasource.druid.max-active")).isEqualTo("16");
        assertThat(environment.getProperty("spring.datasource.druid.password")).isEqualTo("测试数据库密码");
    }
}

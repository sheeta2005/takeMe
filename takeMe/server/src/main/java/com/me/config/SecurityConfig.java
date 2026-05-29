package com.me.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class SecurityConfig {

    /**
     * 密码编码器配置
     * 算法：BCrypt（Spring官方推荐，自带随机盐，防彩虹表攻击）
     * 强度：12（生产环境标准值，单次验证约300ms，平衡安全性和性能）
     * 返回接口类型而非具体实现，便于后续更换算法
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
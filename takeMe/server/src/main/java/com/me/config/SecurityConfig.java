package com.me.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Slf4j
@Configuration
@EnableWebSecurity
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

    /**
     * 放行所有接口，允许登录访问（新增，解决被拦截问题）
     */
    /**
     * Spring Security 放行配置（SB3.2.5 标准写法）
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.info("=== SecurityConfig 正在加载 ===");
        http
                // 关闭 CSRF（前后端分离必须关）
                .csrf(CsrfConfigurer::disable)

                // 放行所有请求，不拦截
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                );
        return http.build();
    }
}
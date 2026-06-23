package com.me.config;

import com.me.interceptor.JwtTokenAdminInterceptor;
import com.me.interceptor.JwtTokenVolunteerInterceptor;
import com.me.interceptor.JwtTokenUserInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC配置类
 * 注册所有拦截器并配置拦截路径
 * 与前端API格式统一：/api/admin/**、/api/volunteer/**、/api/user/**
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtTokenAdminInterceptor adminInterceptor;
    private final JwtTokenVolunteerInterceptor volunteerInterceptor;
    private final JwtTokenUserInterceptor userInterceptor;

    // 构造器注入
    public WebMvcConfig(JwtTokenAdminInterceptor adminInterceptor,
                                         JwtTokenVolunteerInterceptor volunteerInterceptor,
                                         JwtTokenUserInterceptor userInterceptor) {
        this.adminInterceptor = adminInterceptor;
        this.volunteerInterceptor = volunteerInterceptor;
        this.userInterceptor = userInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 管理员拦截器：拦截所有 /api/admin/**
        registry.addInterceptor(adminInterceptor)
                .addPathPatterns("/api/admin/**")
                .excludePathPatterns(
                        "/api/admin/login"
                );

        // 志愿者拦截器：拦截所有 /api/volunteer/**
        registry.addInterceptor(volunteerInterceptor)
                .addPathPatterns("/api/volunteer/**")
                .excludePathPatterns(
                        "/api/volunteer/login",
                        "/api/volunteer/register"
                );

        // 普通用户拦截器：拦截所有 /api/user/**
        registry.addInterceptor(userInterceptor)
                .addPathPatterns("/api/user/**")
                .excludePathPatterns(
                        "/api/user/login",
                        "/api/user/register"
                );
    }
}
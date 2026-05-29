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
        // 管理员拦截器：拦截所有 /admin/**
        registry.addInterceptor(adminInterceptor)
                .addPathPatterns("/admin/**")
                .excludePathPatterns(
                        "/admin/login",
                        "/login",
                        "/register"
                );

        // 志愿者拦截器：拦截所有 /volunteer/**（和你现在的路径完全一致）
        registry.addInterceptor(volunteerInterceptor)
                .addPathPatterns("/volunteer/**")
                .excludePathPatterns(
                        "/volunteer/login",
                        "/login",
                        "/register"
                );

        // 普通用户拦截器：拦截所有 /user/**
        registry.addInterceptor(userInterceptor)
                .addPathPatterns("/user/**")
                .excludePathPatterns(
                        "/user/login",
                        "/login",
                        "/register"
                );
    }
}
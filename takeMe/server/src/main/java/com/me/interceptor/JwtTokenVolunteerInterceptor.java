package com.me.interceptor;

import com.me.context.BaseContext;
import com.me.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 志愿者端JWT令牌校验拦截器
 * 拦截路径：/vol/**
 * 角色要求：1=志愿者
 */
@Component
public class JwtTokenVolunteerInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    public JwtTokenVolunteerInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        String token = request.getHeader("token");

        try {
            Long userId = jwtUtil.getUserId(token);
            Integer role = jwtUtil.getRole(token);

            // 校验角色必须是志愿者（1）
            if (role == null || role != 1) {
                response.setStatus(401);
                return false;
            }

            BaseContext.setLoginId(userId);
            BaseContext.setLoginType(role);
            return true;
        } catch (Exception ex) {
            response.setStatus(401);
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        BaseContext.clear();
    }
}
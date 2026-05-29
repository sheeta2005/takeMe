package com.me.interceptor;

import com.me.context.BaseContext;
import com.me.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 用户端JWT令牌校验拦截器
 * 拦截路径：/user/**
 * 角色要求：2=普通用户（老人）
 */
@Component
public class JwtTokenUserInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    public JwtTokenUserInterceptor(JwtUtil jwtUtil) {
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

            // 校验角色必须是普通用户（2）
            if (role == null || role != 2) {
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
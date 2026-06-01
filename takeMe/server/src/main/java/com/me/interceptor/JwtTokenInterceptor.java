package com.me.interceptor;

import com.me.utils.JwtUtil;
import com.me.context.BaseContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * 通用JWT登录拦截器
 * 适配：Spring Boot 3.2.5 + Java 21（jakarta.servlet）
 * 功能：校验token、解析userId和role、存入ThreadLocal、请求结束清理
 */
@Component
public class JwtTokenInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    public JwtTokenInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        // 1. 获取请求头中的 Authorization（标准头）
        String authHeader = request.getHeader("Authorization");
        String token = null;

        // 2. 解析 token：支持 "Bearer <token>" 或 直接 token 字符串
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7); // 去掉 "Bearer "
        } else if (authHeader != null) {
            token = authHeader; // 兼容直接传 token
        }

        // 3. token 非空校验
        if (token == null || token.trim().isEmpty()) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"msg\":\"未登录，请先登录\"}");
            return false;
        }

        // 4. 解析 token
        Long userId = jwtUtil.getUserId(token);
        Integer role = jwtUtil.getRole(token);

        // 5. 存入 ThreadLocal
        BaseContext.setLoginId(userId);
        BaseContext.setLoginType(role);

        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request,
                           HttpServletResponse response,
                           Object handler,
                           ModelAndView modelAndView) throws Exception {
        // 一般不用
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) throws Exception {
        BaseContext.clear();
    }
}
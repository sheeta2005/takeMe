package com.me.interceptor;

import com.me.context.BaseContext;
import com.me.utils.JwtUtil;
import com.me.service.AccountAccessService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class JwtTokenVolunteerInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final AccountAccessService accountAccessService;

    //初始化志愿者鉴权依赖
    public JwtTokenVolunteerInterceptor(JwtUtil jwtUtil, AccountAccessService accountAccessService) {
        this.jwtUtil = jwtUtil;
        this.accountAccessService = accountAccessService;
    }


    //校验志愿者登录令牌
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        String authHeader = request.getHeader("Authorization");
        String token = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        } else if (authHeader != null) {
            token = authHeader;
        }

        try {
            Long userId = jwtUtil.getUserId(token);
            Integer role = jwtUtil.getRole(token);

            if (role == null || role != 1 || !accountAccessService.isActive(userId, role)) {
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

    //清理请求登录信息
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        BaseContext.clear();
    }
}

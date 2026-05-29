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

    // 构造注入（Spring 3.x推荐）
    public JwtTokenInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    /**
     * 前置拦截：Controller执行之前
     */
    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        // 1. 获取请求头中的token（你约定的header名：token）
        String token = request.getHeader("token");

        // 2. token非空校验
        if (token == null || token.trim().isEmpty()) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"msg\":\"未登录，请先登录\"}");
            return false;
        }

        // 3. 解析token（异常在JwtUtil里已捕获抛Runtime）
        Long userId = jwtUtil.getUserId(token);
        Integer role = jwtUtil.getRole(token);

        // 4. 角色转Integer（你BaseContext里是0=管理员、1=志愿者、2=普通用户）
        Integer loginType;
        try {
            loginType = role;
        } catch (NumberFormatException e) {
            response.setStatus(401);
            response.getWriter().write("{\"code\":401,\"msg\":\"token角色格式错误\"}");
            return false;
        }

        // 5. 存入ThreadLocal（BaseContext）
        BaseContext.setLoginId(userId);
        BaseContext.setLoginType(loginType);

        // 放行
        return true;
    }

    /**
     * Controller执行后、视图渲染前
     */
    @Override
    public void postHandle(HttpServletRequest request,
                           HttpServletResponse response,
                           Object handler,
                           ModelAndView modelAndView) throws Exception {
        // 一般不用
    }

    /**
     * 整个请求完毕（视图渲染后），无论成功失败都会执行
     * 核心：清理ThreadLocal，防止内存泄漏
     */
    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) throws Exception {
        BaseContext.clear();
    }
}
//package com.me.interceptor;
//
//import com.me.utils.BaseContext;
//import com.me.utils.JwtUtil;
//import io.jsonwebtoken.Claims;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.web.servlet.HandlerInterceptor;
//
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
//
//public class JwtTokenInterceptor implements HandlerInterceptor {
//
//    @Value("${takeMe.jwt.secret-key}")
//    private String secretKey;
//
//    @Override
//    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
//        // 从请求头获取token
//        String token = request.getHeader("token");
//        if (token == null || token.isEmpty()) {
//            response.setStatus(401);
//            return false;
//        }
//
//        // 解析token（使用parserBuilder，适配新版jjwt）
//        Claims claims = JwtUtil.parseJWT(secretKey, token);
//
//        Long userId = Long.valueOf(claims.get("userId").toString());
//        String role = claims.get("role").toString();
//
//        // 存入ThreadLocal
//        BaseContext.setUserId(userId);
//        BaseContext.setUserRole(role);
//
//        return true;
//    }
//
//    @Override
//    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
//        // 清除ThreadLocal，防止内存泄漏
//        BaseContext.remove();
//    }
//}
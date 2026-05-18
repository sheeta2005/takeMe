//package com.sky.interceptor;
//
//import com.sky.constant.JwtClaimsConstant;
//import com.sky.context.BaseContext;
//import com.sky.properties.JwtProperties;
//import com.sky.utils.JwtUtil;
//import io.jsonwebtoken.Claims;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//import org.springframework.web.method.HandlerMethod;
//import org.springframework.web.servlet.HandlerInterceptor;
//
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
//
///**
// * 用户端jwt令牌校验的拦截器（小程序用户）
// */
//@Component
//@Slf4j
//public class JwtTokenUserInterceptor implements HandlerInterceptor {
//
//    @Autowired
//    private JwtProperties jwtProperties;
//
//    /**
//     * 校验jwt
//     *
//     * @param request
//     * @param response
//     * @param handler
//     * @return
//     * @throws Exception
//     */
//    @Override // 补充@Override注解，规范写法
//    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
//        //判断当前拦截到的是Controller的方法还是其他资源
//        if (!(handler instanceof HandlerMethod)) {
//            //当前拦截到的不是动态方法，直接放行
//            return true;
//        }
//        //1、从请求头中获取用户端token（注意和管理端的token名称区分）
//        String token = request.getHeader(jwtProperties.getUserTokenName()); // 关键：用userTokenName
//        //2、校验令牌
//        try {
//            log.info("用户端jwt校验:{}", token);
//            // 关键：用用户端的秘钥解析token
//            Claims claims = JwtUtil.parseJWT(jwtProperties.getUserSecretKey(), token);
//            // 关键：获取用户ID（不是员工ID）
//            Long userId = Long.valueOf(claims.get(JwtClaimsConstant.USER_ID).toString());
//            log.info("当前用户id：{}", userId); // 补充{}，否则日志不显示ID
//            BaseContext.setCurrentId(userId); // 存入ThreadLocal
//            //3、通过，放行
//            return true;
//        } catch (Exception ex) {
//            //4、不通过，响应401状态码
//            response.setStatus(401);
//            return false;
//        }
//    }
//
//    // 关键：请求完成后清理ThreadLocal，避免内存泄漏
//    @Override
//    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
//        BaseContext.removeCurrentId();
//    }
//}
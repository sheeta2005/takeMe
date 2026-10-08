package com.me.redis.aspect;

import com.me.context.BaseContext;
import com.me.dto.LoginDTO;
import com.me.redis.annotation.RateLimit;
import com.me.redis.utils.RedisUtil;
import com.me.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
@ConditionalOnExpression("${middleware.enabled:true} && ${middleware.redis.enabled:true}")
public class RateLimitAspect {

    private final RedisUtil redisUtil;
    @Value("${rate-limit.login-ip-count:1000}")
    private int loginIpCount;

    //拦截并限制请求频率
    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        String identifier = resolveIdentifier(joinPoint);
        String key = rateLimit.prefix() + ":" + identifier;

        boolean allowed = redisUtil.rateLimit(key, rateLimit.period(), rateLimit.count());
        boolean ipAllowed = true;
        if (BaseContext.getLoginId() == null && rateLimit.prefix().endsWith(":login")) {
            // 小区出口 IP 可能由数百名老人共享，只作较宽松的辅助防护。
            ipAllowed = redisUtil.rateLimit("rate:login:ip:" + getIp(), 60, loginIpCount);
        }
        if (!allowed || !ipAllowed) {
            log.warn("限流拦截: key={}, limit={}/{}/{}s", key, rateLimit.count(), rateLimit.period(), identifier);
            return Result.error(429, "请求过于频繁，请稍后再试");
        }

        return joinPoint.proceed();
    }

    //获取账号限流标识
    private String resolveIdentifier(ProceedingJoinPoint joinPoint) {
        Long userId = BaseContext.getLoginId();
        if (userId != null) {
            return String.valueOf(userId);
        }
        for (Object arg : joinPoint.getArgs()) {
            if (arg instanceof LoginDTO login && login.getUsername() != null) {
                String account = login.getUsername().strip().toLowerCase(java.util.Locale.ROOT);
                return "account:" + java.util.UUID.nameUUIDFromBytes(
                        account.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
        }
        return getIp();
    }

    //获取请求来源地址
    private String getIp() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return "unknown";
        }
        HttpServletRequest request = attrs.getRequest();
        // 未配置可信代理时不能信任客户端自行提供的转发头。
        return request.getRemoteAddr();
    }
}

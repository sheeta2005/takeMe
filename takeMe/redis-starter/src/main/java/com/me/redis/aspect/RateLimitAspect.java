package com.me.redis.aspect;

import com.me.context.BaseContext;
import com.me.redis.annotation.RateLimit;
import com.me.redis.utils.RedisUtil;
import com.me.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final RedisUtil redisUtil;

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        String identifier = resolveIdentifier();
        String key = rateLimit.prefix() + ":" + identifier;

        boolean allowed = redisUtil.rateLimit(key, rateLimit.period(), rateLimit.count());
        if (!allowed) {
            log.warn("限流拦截: key={}, limit={}/{}/{}s", key, rateLimit.count(), rateLimit.period(), identifier);
            return Result.error(429, "请求过于频繁，请稍后再试");
        }

        return joinPoint.proceed();
    }

    private String resolveIdentifier() {
        Long userId = BaseContext.getLoginId();
        if (userId != null) {
            return String.valueOf(userId);
        }
        return getIp();
    }

    private String getIp() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return "unknown";
        }
        HttpServletRequest request = attrs.getRequest();
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            int commaIdx = ip.indexOf(',');
            if (commaIdx > 0) {
                ip = ip.substring(0, commaIdx);
            }
            return ip.trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip.trim();
        }
        return request.getRemoteAddr();
    }
}

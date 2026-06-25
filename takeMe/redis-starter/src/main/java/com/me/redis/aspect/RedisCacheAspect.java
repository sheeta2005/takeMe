package com.me.redis.aspect;

import com.me.redis.annotation.RedisCache;
import com.me.redis.utils.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "middleware.redis.enabled", havingValue = "true", matchIfMissing = true)
public class RedisCacheAspect {

    private final RedisUtil redisUtil;

    @Around("@annotation(redisCache)")
    public Object around(ProceedingJoinPoint joinPoint, RedisCache redisCache) throws Throwable {
        String cacheKey = buildCacheKey(joinPoint, redisCache.prefix(), redisCache.keyArgs());

        if (redisUtil.isNullCached(cacheKey)) {
            log.debug("缓存穿透防护: 空值缓存命中 key={}", cacheKey);
            return null;
        }

        Object cached = redisUtil.get(cacheKey);
        if (cached != null) {
            log.debug("缓存命中 key={}", cacheKey);
            return cached;
        }

        Object result = joinPoint.proceed();

        if (result == null) {
            redisUtil.setNull(cacheKey, redisCache.nullExpire(), TimeUnit.MINUTES);
            log.debug("空值缓存设置 key={}, expire={}min", cacheKey, redisCache.nullExpire());
            return null;
        }

        redisUtil.set(cacheKey, result, redisCache.expire(), TimeUnit.MINUTES);
        log.debug("缓存写入成功 key={}, expire={}min", cacheKey, redisCache.expire());

        return result;
    }

    private String buildCacheKey(ProceedingJoinPoint joinPoint, String prefix, int[] keyArgs) {
        StringBuilder key = new StringBuilder(prefix);
        if (keyArgs.length > 0) {
            Object[] args = joinPoint.getArgs();
            for (int i : keyArgs) {
                if (i < args.length) {
                    key.append(":").append(args[i]);
                }
            }
        }
        return key.toString();
    }
}

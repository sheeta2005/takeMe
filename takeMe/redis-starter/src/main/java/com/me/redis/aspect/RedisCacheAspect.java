package com.me.redis.aspect;

import com.me.redis.annotation.RedisCache;
import com.me.redis.utils.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
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
            log.debug("缓存穿透防护: 数据库无数据，写入空值缓存 key={}, ttl={}min", cacheKey, redisCache.nullExpire());
        } else {
            redisUtil.set(cacheKey, result, redisCache.expire(), TimeUnit.MINUTES);
            log.debug("缓存写入 key={}, ttl={}min", cacheKey, redisCache.expire());
        }

        return result;
    }

    private String buildCacheKey(ProceedingJoinPoint joinPoint, String prefix, int[] keyArgs) {
        Object[] args = joinPoint.getArgs();
        StringBuilder keyBuilder = new StringBuilder(prefix);
        if (keyArgs.length > 0) {
            for (int idx : keyArgs) {
                if (idx < args.length && args[idx] != null) {
                    keyBuilder.append(":").append(args[idx]);
                }
            }
        } else {
            for (Object arg : args) {
                if (arg != null) {
                    keyBuilder.append(":").append(arg);
                }
            }
        }
        return keyBuilder.toString();
    }
}

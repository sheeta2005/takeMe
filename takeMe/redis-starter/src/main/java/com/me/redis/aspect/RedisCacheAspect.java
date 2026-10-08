package com.me.redis.aspect;

import com.me.redis.annotation.RedisCache;
import com.me.redis.utils.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.serializer.SerializationException;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
@ConditionalOnExpression("${middleware.enabled:true} && ${middleware.redis.enabled:true}")
public class RedisCacheAspect {

    private final RedisUtil redisUtil;
    private static final Object MISS = new Object();
    private static final Object NULL_VALUE = new Object();

    //优先读取缓存并合并回源
    @Around("@annotation(redisCache)")
    public Object around(ProceedingJoinPoint joinPoint, RedisCache redisCache) throws Throwable {
        String cacheKey = buildCacheKey(joinPoint, redisCache.prefix(), redisCache.keyArgs());

        Object cached = read(cacheKey);
        if (cached != MISS) return cached == NULL_VALUE ? null : cached;
        // 部署为单 JVM，固定数量锁条带合并热点回源，并与目录缓存失效协调。
        synchronized (redisUtil.cacheMonitor(cacheKey)) {
            // 等待者再次查缓存，避免所有线程排队后各自查询数据库。
            cached = read(cacheKey);
            if (cached != MISS) return cached == NULL_VALUE ? null : cached;
            Object result = joinPoint.proceed();
            try {
                if (result == null) {
                    redisUtil.setNull(cacheKey, jitterSeconds(redisCache.nullExpire()), TimeUnit.SECONDS);
                } else {
                    redisUtil.set(cacheKey, result, jitterSeconds(redisCache.expire()), TimeUnit.SECONDS);
                }
            } catch (DataAccessException | SerializationException ex) {
                // 仅可重建的查询缓存降级；数据库异常和安全限流不在此处吞掉。
                log.warn("查询缓存写入失败，保留数据库结果 key={}, 原因={}", cacheKey, ex.getClass().getSimpleName());
            }
            return result;
        }
    }

    //读取缓存及空值标记
    private Object read(String key) {
        try {
            Object value = redisUtil.get(key);
            if (value != null) return value;
            return redisUtil.isNullCached(key) ? NULL_VALUE : MISS;
        } catch (DataAccessException | SerializationException ex) {
            log.warn("查询缓存读取失败，回源数据库 key={}, 原因={}", key, ex.getClass().getSimpleName());
            return MISS;
        }
    }

    //计算带随机偏移的缓存时长
    private long jitterSeconds(long minutes) {
        // 注解仍以分钟配置；实际 TTL 加减 10%，避免一批键同时到期。
        long seconds = Math.max(1, TimeUnit.MINUTES.toSeconds(minutes));
        long spread = seconds / 10;
        return Math.max(1, seconds + ThreadLocalRandom.current().nextLong(-spread, spread + 1));
    }

    //按指定参数生成缓存键
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

package com.me.redis.aspect;

import com.me.redis.annotation.RedisLock;
import com.me.redis.utils.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RedisLockAspect {

    private final RedisUtil redisUtil;

    @Around("@annotation(redisLock)")
    public Object around(ProceedingJoinPoint joinPoint, RedisLock redisLock) throws Throwable {
        String lockKey = buildLockKey(joinPoint, redisLock.prefix(), redisLock.keyArgs());
        String lockValue = UUID.randomUUID().toString();

        Boolean acquired = redisUtil.setIfAbsent(lockKey, lockValue, redisLock.timeout(), TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(acquired)) {
            log.warn("分布式锁获取失败: key={}", lockKey);
            throw new RuntimeException("操作过于频繁，请稍后再试");
        }

        try {
            return joinPoint.proceed();
        } finally {
            boolean released = redisUtil.releaseLock(lockKey, lockValue);
            if (released) {
                log.debug("分布式锁释放成功: key={}", lockKey);
            } else {
                log.warn("分布式锁释放失败(可能已过期): key={}", lockKey);
            }
        }
    }

    private String buildLockKey(ProceedingJoinPoint joinPoint, String prefix, int[] keyArgs) {
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

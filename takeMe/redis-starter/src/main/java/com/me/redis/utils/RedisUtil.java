package com.me.redis.utils;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class RedisUtil {

    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String NULL_CACHE_PREFIX = "null:";

    private static final String RELEASE_LOCK_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";

    private static final String RATE_LIMIT_SCRIPT =
            "local exists = redis.call('EXISTS', KEYS[1]) " +
            "if exists == 1 then " +
            "  local val = redis.call('GET', KEYS[1]) " +
            "  if not tonumber(val) then " +
            "    redis.call('DEL', KEYS[1]) " +
            "  end " +
            "end " +
            "local count = redis.call('INCR', KEYS[1]) " +
            "if count == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end " +
            "if count > tonumber(ARGV[2]) then return 0 else return 1 end";

    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public void set(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public Boolean delete(String key) {
        return redisTemplate.delete(key);
    }

    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    public Boolean expire(String key, long timeout, TimeUnit unit) {
        return redisTemplate.expire(key, timeout, unit);
    }

    public Long getExpire(String key) {
        return redisTemplate.getExpire(key);
    }

    public void increment(String key, long delta) {
        redisTemplate.opsForValue().increment(key, delta);
    }

    public void decrement(String key, long delta) {
        redisTemplate.opsForValue().decrement(key, delta);
    }

    public Boolean setIfAbsent(String key, Object value, long timeout, TimeUnit unit) {
        return redisTemplate.opsForValue().setIfAbsent(key, value, timeout, unit);
    }

    public boolean releaseLock(String lockKey, String lockValue) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(RELEASE_LOCK_SCRIPT, Long.class);
        Long result = redisTemplate.execute(script, Collections.singletonList(lockKey), lockValue);
        return Long.valueOf(1).equals(result);
    }

    public boolean rateLimit(String key, int period, int maxCount) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(RATE_LIMIT_SCRIPT, Long.class);
        Long result = stringRedisTemplate.execute(script, Collections.singletonList(key), String.valueOf(period), String.valueOf(maxCount));
        return Long.valueOf(1).equals(result);
    }

    public Set<String> keys(String pattern) {
        return redisTemplate.keys(pattern);
    }

    public void setNull(String key, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(NULL_CACHE_PREFIX + key, "", timeout, unit);
    }

    public boolean isNullCached(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(NULL_CACHE_PREFIX + key));
    }
}

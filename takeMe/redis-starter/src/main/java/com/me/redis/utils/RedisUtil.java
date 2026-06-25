package com.me.redis.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisUtil {

    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    @Value("${middleware.redis.enabled:true}")
    private boolean redisEnabled;

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
        if (!redisEnabled) {
            log.debug("Redis 已禁用，跳过 set 操作 key={}", key);
            return;
        }
        redisTemplate.opsForValue().set(key, value);
    }

    public void set(String key, Object value, long timeout, TimeUnit unit) {
        if (!redisEnabled) {
            log.debug("Redis 已禁用，跳过 set 操作 key={}", key);
            return;
        }
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    public Object get(String key) {
        if (!redisEnabled) {
            log.debug("Redis 已禁用，跳过 get 操作 key={}", key);
            return null;
        }
        return redisTemplate.opsForValue().get(key);
    }

    public Boolean delete(String key) {
        if (!redisEnabled) {
            log.debug("Redis 已禁用，跳过 delete 操作 key={}", key);
            return false;
        }
        return redisTemplate.delete(key);
    }

    public Boolean hasKey(String key) {
        if (!redisEnabled) {
            log.debug("Redis 已禁用，跳过 hasKey 操作 key={}", key);
            return false;
        }
        return redisTemplate.hasKey(key);
    }

    public Boolean expire(String key, long timeout, TimeUnit unit) {
        if (!redisEnabled) {
            log.debug("Redis 已禁用，跳过 expire 操作 key={}", key);
            return false;
        }
        return redisTemplate.expire(key, timeout, unit);
    }

    public Long getExpire(String key) {
        if (!redisEnabled) {
            log.debug("Redis 已禁用，跳过 getExpire 操作 key={}", key);
            return -1L;
        }
        return redisTemplate.getExpire(key);
    }

    public void increment(String key, long delta) {
        if (!redisEnabled) {
            log.debug("Redis 已禁用，跳过 increment 操作 key={}", key);
            return;
        }
        redisTemplate.opsForValue().increment(key, delta);
    }

    public void decrement(String key, long delta) {
        if (!redisEnabled) {
            log.debug("Redis 已禁用，跳过 decrement 操作 key={}", key);
            return;
        }
        redisTemplate.opsForValue().decrement(key, delta);
    }

    public Boolean setIfAbsent(String key, Object value, long timeout, TimeUnit unit) {
        if (!redisEnabled) {
            log.debug("Redis 已禁用，跳过 setIfAbsent 操作 key={}", key);
            return true;
        }
        return redisTemplate.opsForValue().setIfAbsent(key, value, timeout, unit);
    }

    public boolean releaseLock(String lockKey, String lockValue) {
        if (!redisEnabled) {
            log.debug("Redis 已禁用，跳过 releaseLock 操作 key={}", lockKey);
            return true;
        }
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(RELEASE_LOCK_SCRIPT, Long.class);
        Long result = redisTemplate.execute(script, Collections.singletonList(lockKey), lockValue);
        return Long.valueOf(1).equals(result);
    }

    public boolean rateLimit(String key, int period, int maxCount) {
        if (!redisEnabled) {
            log.debug("Redis 已禁用，跳过 rateLimit 检查 key={}", key);
            return true;
        }
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(RATE_LIMIT_SCRIPT, Long.class);
        Long result = stringRedisTemplate.execute(script, Collections.singletonList(key), String.valueOf(period), String.valueOf(maxCount));
        return Long.valueOf(1).equals(result);
    }

    public Set<String> keys(String pattern) {
        if (!redisEnabled) {
            log.debug("Redis 已禁用，跳过 keys 操作 pattern={}", pattern);
            return Collections.emptySet();
        }
        return redisTemplate.keys(pattern);
    }

    public void deleteByPattern(String pattern) {
        if (!redisEnabled) {
            log.debug("Redis 已禁用，跳过 deleteByPattern 操作 pattern={}", pattern);
            return;
        }
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.info("批量删除缓存: pattern={}, count={}", pattern, keys.size());
        }
    }

    public void setNull(String key, long timeout, TimeUnit unit) {
        if (!redisEnabled) {
            log.debug("Redis 已禁用，跳过 setNull 操作 key={}", key);
            return;
        }
        redisTemplate.opsForValue().set(NULL_CACHE_PREFIX + key, "", timeout, unit);
    }

    public boolean isNullCached(String key) {
        if (!redisEnabled) {
            log.debug("Redis 已禁用，跳过 isNullCached 检查 key={}", key);
            return false;
        }
        return Boolean.TRUE.equals(redisTemplate.hasKey(NULL_CACHE_PREFIX + key));
    }

    public void hSet(String key, String field, Object value) {
        if (!redisEnabled) {
            log.debug("Redis 已禁用，跳过 hSet 操作 key={}, field={}", key, field);
            return;
        }
        redisTemplate.opsForHash().put(key, field, value);
    }

    public Object hGet(String key, String field) {
        if (!redisEnabled) {
            log.debug("Redis 已禁用，跳过 hGet 操作 key={}, field={}", key, field);
            return null;
        }
        return redisTemplate.opsForHash().get(key, field);
    }

    public Long hIncrBy(String key, String field, long increment) {
        if (!redisEnabled) {
            log.debug("Redis 已禁用，跳过 hIncrBy 操作 key={}, field={}", key, field);
            return 0L;
        }
        return redisTemplate.opsForHash().increment(key, field, increment);
    }

    public void hDel(String key, String... fields) {
        if (!redisEnabled) {
            log.debug("Redis 已禁用，跳过 hDel 操作 key={}", key);
            return;
        }
        redisTemplate.opsForHash().delete(key, (Object[]) fields);
    }
}

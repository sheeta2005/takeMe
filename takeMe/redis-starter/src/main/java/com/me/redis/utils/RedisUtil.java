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

    @Value("#{${middleware.enabled:true} && ${middleware.redis.enabled:true}}")
    private boolean redisEnabled;

    private static final String NULL_CACHE_PREFIX = "null:";
    private final Object[] cacheLocks = java.util.stream.IntStream.range(0, 64)
            .mapToObj(i -> new Object()).toArray();

    //锁数组
    public Object cacheMonitor(String key) {
        // 回源与主动失效共用本机锁，防止旧查询在目录修改提交之后重新写入过时缓存。
        String businessKey = key.startsWith(NULL_CACHE_PREFIX) ? key.substring(NULL_CACHE_PREFIX.length()) : key;
        return cacheLocks[Math.floorMod(businessKey.hashCode(), cacheLocks.length)];
    }

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

    //写入缓存
    public void set(String key, Object value) {
        if (!redisEnabled) {
            log.debug("Redis 已禁用，跳过 set 操作 key={}", key);
            return;
        }
        redisTemplate.opsForValue().set(key, value);
    }

    //写入缓存
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        if (!redisEnabled) {
            log.debug("Redis 已禁用，跳过 set 操作 key={}", key);
            return;
        }
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    //读取缓存
    public Object get(String key) {
        if (!redisEnabled) {
            log.debug("Redis 已禁用，跳过 get 操作 key={}", key);
            return null;
        }
        return redisTemplate.opsForValue().get(key);
    }

    //删除缓存
    public Boolean delete(String key) {
        if (!redisEnabled) {
            log.debug("Redis 已禁用，跳过 delete 操作 key={}", key);
            return false;
        }
        synchronized (cacheMonitor(key)) {
            return redisTemplate.delete(key);
        }
    }

    //判断缓存键是否存在
    public Boolean hasKey(String key) {
        if (!redisEnabled) {
            log.debug("Redis 已禁用，跳过 hasKey 操作 key={}", key);
            return false;
        }
        return redisTemplate.hasKey(key);
    }

    //设置缓存有效期
    public Boolean expire(String key, long timeout, TimeUnit unit) {
        if (!redisEnabled) {
            log.debug("Redis 已禁用，跳过 expire 操作 key={}", key);
            return false;
        }
        return redisTemplate.expire(key, timeout, unit);
    }

    //获取缓存剩余秒数
    public Long getExpire(String key) {
        if (!redisEnabled) {
            log.debug("Redis 已禁用，跳过 getExpire 操作 key={}", key);
            return -1L;
        }
        return redisTemplate.getExpire(key);
    }

    //递增缓存数值
    public void increment(String key, long delta) {
        if (!redisEnabled) {
            log.debug("Redis 已禁用，跳过 increment 操作 key={}", key);
            return;
        }
        redisTemplate.opsForValue().increment(key, delta);
    }

    //递减缓存数值
    public void decrement(String key, long delta) {
        if (!redisEnabled) {
            log.debug("Redis 已禁用，跳过 decrement 操作 key={}", key);
            return;
        }
        redisTemplate.opsForValue().decrement(key, delta);
    }

    //仅在键不存在时写入缓存
    public Boolean setIfAbsent(String key, Object value, long timeout, TimeUnit unit) {
        if (!redisEnabled) {
            log.debug("Redis 已禁用，跳过 setIfAbsent 操作 key={}", key);
            return true;
        }
        return redisTemplate.opsForValue().setIfAbsent(key, value, timeout, unit);
    }

    //校验时间窗口内的请求次数
    public boolean rateLimit(String key, int period, int maxCount) {
        if (!redisEnabled) {
            log.debug("Redis 已禁用，跳过 rateLimit 检查 key={}", key);
            return true;
        }
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(RATE_LIMIT_SCRIPT, Long.class);
        Long result = stringRedisTemplate.execute(script, Collections.singletonList(key), String.valueOf(period), String.valueOf(maxCount));
        return Long.valueOf(1).equals(result);
    }

    //查询匹配的缓存键
    public Set<String> keys(String pattern) {
        if (!redisEnabled) {
            log.debug("Redis 已禁用，跳过 keys 操作 pattern={}", pattern);
            return Collections.emptySet();
        }
        return redisTemplate.keys(pattern);
    }

    //删除匹配的缓存键
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

    //缓存空值标记
    public void setNull(String key, long timeout, TimeUnit unit) {
        if (!redisEnabled) {
            log.debug("Redis 已禁用，跳过 setNull 操作 key={}", key);
            return;
        }
        redisTemplate.opsForValue().set(NULL_CACHE_PREFIX + key, "", timeout, unit);
    }

    //判断空值是否已缓存
    public boolean isNullCached(String key) {
        if (!redisEnabled) {
            log.debug("Redis 已禁用，跳过 isNullCached 检查 key={}", key);
            return false;
        }
        return Boolean.TRUE.equals(redisTemplate.hasKey(NULL_CACHE_PREFIX + key));
    }

    //写入哈希字段
    public void hSet(String key, String field, Object value) {
        if (!redisEnabled) {
            log.debug("Redis 已禁用，跳过 hSet 操作 key={}, field={}", key, field);
            return;
        }
        redisTemplate.opsForHash().put(key, field, value);
    }

    //读取哈希字段
    public Object hGet(String key, String field) {
        if (!redisEnabled) {
            log.debug("Redis 已禁用，跳过 hGet 操作 key={}, field={}", key, field);
            return null;
        }
        return redisTemplate.opsForHash().get(key, field);
    }

    //递增哈希字段数值
    public Long hIncrBy(String key, String field, long increment) {
        if (!redisEnabled) {
            log.debug("Redis 已禁用，跳过 hIncrBy 操作 key={}, field={}", key, field);
            return 0L;
        }
        return redisTemplate.opsForHash().increment(key, field, increment);
    }

    //删除哈希字段
    public void hDel(String key, String... fields) {
        if (!redisEnabled) {
            log.debug("Redis 已禁用，跳过 hDel 操作 key={}", key);
            return;
        }
        redisTemplate.opsForHash().delete(key, (Object[]) fields);
    }
}

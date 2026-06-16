package com.me;

import com.me.entity.Order;
import com.me.entity.User;
import com.me.redis.utils.RedisUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = TakeMeApplication.class)
public class RedisFeatureTest {

    @Autowired
    private RedisUtil redisUtil;

    @BeforeEach
    void cleanup() {
        redisUtil.delete("test:conn");
        redisUtil.delete("test:cache:user:1");
        redisUtil.delete("null:test:cache:user:1");
        redisUtil.delete("test:lock:1");
        redisUtil.delete("test:lock:2");
        redisUtil.delete("test:lock:3");
        redisUtil.delete("test:limit:ip:127.0.0.1");
    }

    // ==================== 1. Redis连接 ====================
    @Test
    @DisplayName("Redis连接测试")
    void testConnection() {
        redisUtil.set("test:conn", "OK", 1, TimeUnit.MINUTES);
        Object val = redisUtil.get("test:conn");
        assertEquals("OK", val, "Redis连接应正常读写");
        System.out.println("✅ Redis连接正常，读取结果：" + val);
    }

    // ==================== 2. LocalDateTime序列化 ====================
    @Test
    @DisplayName("LocalDateTime序列化测试")
    void testLocalDateTimeSerialization() {
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setRealName("测试老人");
        user.setCreateTime(LocalDateTime.now());

        redisUtil.set("test:cache:user:1", user, 2, TimeUnit.MINUTES);
        Object cached = redisUtil.get("test:cache:user:1");
        assertNotNull(cached, "含LocalDateTime的对象应能正常序列化缓存");
        System.out.println("✅ LocalDateTime序列化正常，缓存对象：" + cached.getClass().getSimpleName());
    }

    // ==================== 3. 缓存穿透防护 ====================
    @Test
    @DisplayName("空值缓存（防穿透）测试")
    void testNullCachePenetration() {
        redisUtil.setNull("test:cache:user:1", 2, TimeUnit.MINUTES);
        assertTrue(redisUtil.isNullCached("test:cache:user:1"), "空值应被标记为已缓存");
        System.out.println("✅ 缓存穿透防护正常，2分钟内重复查DB不会再穿透");
    }

    // ==================== 4. 分布式锁 ====================
    @Test
    @DisplayName("分布式锁：加锁+释放+防误删")
    void testDistributedLock() {
        String lockKey = "test:lock:1";
        String lockValue = UUID.randomUUID().toString();

        Boolean acquired = redisUtil.setIfAbsent(lockKey, lockValue, 5, TimeUnit.SECONDS);
        assertTrue(Boolean.TRUE.equals(acquired), "首次加锁应成功");
        System.out.println("✅ 加锁成功");

        Boolean secondTry = redisUtil.setIfAbsent(lockKey, "other-value", 5, TimeUnit.SECONDS);
        assertTrue(Boolean.FALSE.equals(secondTry), "重复加锁应失败");
        System.out.println("✅ 重复加锁被拒绝");

        boolean wrongRelease = redisUtil.releaseLock(lockKey, "wrong-value");
        assertFalse(wrongRelease, "错误锁值释放应失败");
        assertTrue(Boolean.TRUE.equals(redisUtil.hasKey(lockKey)), "锁应仍在");
        System.out.println("✅ 错误锁值无法释放锁（防误删）");

        boolean correctRelease = redisUtil.releaseLock(lockKey, lockValue);
        assertTrue(correctRelease, "正确锁值释放应成功");
        System.out.println("✅ 正确锁值释放锁成功（Lua脚本原子释放）");
    }

    // ==================== 5. 并发锁 ====================
    @Test
    @DisplayName("并发场景：10线程抢同一把锁")
    void testConcurrentLock() throws InterruptedException {
        String lockKey = "test:lock:3";
        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            pool.submit(() -> {
                String value = UUID.randomUUID().toString();
                Boolean acquired = redisUtil.setIfAbsent(lockKey, value, 5, TimeUnit.SECONDS);
                if (Boolean.TRUE.equals(acquired)) {
                    successCount.incrementAndGet();
                    try { Thread.sleep(500); } catch (InterruptedException e) {}
                    redisUtil.releaseLock(lockKey, value);
                } else {
                    failCount.incrementAndGet();
                }
                latch.countDown();
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        pool.shutdown();

        assertEquals(1, successCount.get(), "并发加锁成功次数应始终为1");
        assertEquals(threadCount - 1, failCount.get(), "失败次数应为线程数-1");
        System.out.println("✅ 并发测试通过：10线程中仅1个加锁成功，其余9个被拒绝");
    }

    // ==================== 6. 限流 ====================
    @Test
    @DisplayName("限流：60秒内最多10次请求")
    void testRateLimit() {
        String key = "test:limit:ip:127.0.0.1";
        int passed = 0;
        int blocked = 0;

        for (int i = 0; i < 15; i++) {
            boolean allowed = redisUtil.rateLimit(key, 60, 10);
            if (allowed) {
                passed++;
            } else {
                blocked++;
            }
        }

        assertEquals(10, passed, "前10次请求应通过");
        assertEquals(5, blocked, "后5次请求应被限流");
        System.out.println("✅ 限流测试通过：10次通过，5次被拦截（INCR+EXPIRE Lua脚本）");
    }
}

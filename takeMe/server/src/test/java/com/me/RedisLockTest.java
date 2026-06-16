//package com.me;
//
//import com.me.redis.utils.RedisUtil;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//
//import java.util.UUID;
//import java.util.concurrent.CountDownLatch;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.concurrent.TimeUnit;
//import java.util.concurrent.atomic.AtomicInteger;
//
//@SpringBootTest(classes = TakeMeApplication.class)
//public class RedisLockTest {
//
//    @Autowired
//    private RedisUtil redisUtil;
//
//    private static final String TEST_LOCK_PREFIX = "test:lock";
//
//    @BeforeEach
//    void cleanup() {
//        for (int i = 0; i <= 10; i++) {
//            redisUtil.delete(TEST_LOCK_PREFIX + ":" + i);
//        }
//    }
//
//    @Test
//    void testSetIfAbsent_and_ReleaseLock() {
//        String lockKey = TEST_LOCK_PREFIX + ":1";
//        String lockValue = UUID.randomUUID().toString();
//
//        Boolean acquired = redisUtil.setIfAbsent(lockKey, lockValue, 5, TimeUnit.SECONDS);
//        System.out.println("加锁结果: " + acquired + " (应为 true)");
//
//        Boolean secondTry = redisUtil.setIfAbsent(lockKey, "other-value", 5, TimeUnit.SECONDS);
//        System.out.println("重复加锁结果: " + secondTry + " (应为 false)");
//
//        boolean released = redisUtil.releaseLock(lockKey, lockValue);
//        System.out.println("释放锁结果: " + released + " (应为 true)");
//
//        Boolean thirdTry = redisUtil.setIfAbsent(lockKey, "new-value", 5, TimeUnit.SECONDS);
//        System.out.println("释放后再加锁: " + thirdTry + " (应为 true)");
//
//        redisUtil.delete(lockKey);
//        redisUtil.delete(TEST_LOCK_PREFIX + ":new-value");
//    }
//
//    @Test
//    void testReleaseLock_wrongValue() {
//        String lockKey = TEST_LOCK_PREFIX + ":2";
//        String lockValue = UUID.randomUUID().toString();
//
//        redisUtil.setIfAbsent(lockKey, lockValue, 5, TimeUnit.SECONDS);
//
//        boolean released = redisUtil.releaseLock(lockKey, "wrong-value");
//        System.out.println("错误锁值释放结果: " + released + " (应为 false，锁仍在)");
//
//        System.out.println("锁仍存在: " + redisUtil.hasKey(lockKey) + " (应为 true)");
//
//        boolean correctRelease = redisUtil.releaseLock(lockKey, lockValue);
//        System.out.println("正确锁值释放结果: " + correctRelease + " (应为 true)");
//
//        redisUtil.delete(lockKey);
//    }
//
//    @Test
//    void testConcurrentLock() throws InterruptedException {
//        String lockKey = TEST_LOCK_PREFIX + ":3";
//        int threadCount = 10;
//        CountDownLatch latch = new CountDownLatch(threadCount);
//        AtomicInteger successCount = new AtomicInteger(0);
//        AtomicInteger failCount = new AtomicInteger(0);
//        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
//
//        for (int i = 0; i < threadCount; i++) {
//            pool.submit(() -> {
//                String value = UUID.randomUUID().toString();
//                Boolean acquired = redisUtil.setIfAbsent(lockKey, value, 5, TimeUnit.SECONDS);
//                if (Boolean.TRUE.equals(acquired)) {
//                    successCount.incrementAndGet();
//                    try { Thread.sleep(1000); } catch (InterruptedException e) {}
//                    redisUtil.releaseLock(lockKey, value);
//                } else {
//                    failCount.incrementAndGet();
//                }
//                latch.countDown();
//            });
//        }
//
//        latch.await(30, TimeUnit.SECONDS);
//        pool.shutdown();
//
//        System.out.println("并发线程数: " + threadCount);
//        System.out.println("加锁成功次数: " + successCount.get() + " (应始终为 1)");
//        System.out.println("加锁失败次数: " + failCount.get() + " (应为 " + (threadCount - 1) + ")");
//    }
//}

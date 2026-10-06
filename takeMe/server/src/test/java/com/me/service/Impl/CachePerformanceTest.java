package com.me.service.Impl;

import com.me.dto.PageResultDTO;
import com.me.redis.annotation.RedisCache;
import com.me.redis.aspect.RedisCacheAspect;
import com.me.redis.utils.RedisUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CachePerformanceTest {
    @RedisCache(prefix = "test:hot", expire = 2)
    public List<String> hot() { return List.of(); }

    private RedisCache annotation() throws Exception {
        return getClass().getMethod("hot").getAnnotation(RedisCache.class);
    }

    private RedisUtil redis() {
        RedisUtil redis = mock(RedisUtil.class);
        when(redis.cacheMonitor(anyString())).thenReturn(new Object());
        return redis;
    }

    @Test
    void concurrentMissLoadsOnlyOnceAndTtlHasJitter() throws Exception {
        RedisUtil redis = redis();
        Map<String, Object> cache = new ConcurrentHashMap<>();
        when(redis.get(anyString())).thenAnswer(call -> cache.get(call.getArgument(0)));
        doAnswer(call -> {
            cache.put(call.getArgument(0), call.getArgument(1));
            return null;
        }).when(redis).set(anyString(), any(), anyLong(), any(TimeUnit.class));
        RedisCacheAspect aspect = new RedisCacheAspect(redis);
        AtomicInteger loads = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService workers = Executors.newFixedThreadPool(20);
        try {
            var futures = new java.util.ArrayList<Future<Object>>();
            for (int i = 0; i < 20; i++) {
                futures.add(workers.submit(() -> {
                    start.await();
                    ProceedingJoinPoint point = mock(ProceedingJoinPoint.class);
                    try {
                        when(point.proceed()).thenAnswer(call -> {
                            loads.incrementAndGet();
                            Thread.sleep(60);
                            return List.of("助餐");
                        });
                        return aspect.around(point, annotation());
                    } catch (Throwable error) {
                        throw new IllegalStateException(error);
                    }
                }));
            }
            start.countDown();
            for (Future<Object> future : futures) assertEquals(List.of("助餐"), future.get(5, TimeUnit.SECONDS));
            assertEquals(1, loads.get());
            verify(redis).set(eq("test:hot"), any(), longThat(ttl -> ttl >= 108 && ttl <= 132), eq(TimeUnit.SECONDS));
        } finally {
            workers.shutdownNow();
        }
    }

    @Test
    void cacheReadFailureDoesNotTurnSuccessfulQueryIntoFailure() throws Throwable {
        RedisUtil redis = redis();
        when(redis.get(anyString())).thenThrow(new RedisConnectionFailureException("测试断连"));
        ProceedingJoinPoint point = mock(ProceedingJoinPoint.class);
        when(point.proceed()).thenReturn(List.of("助餐"));
        assertEquals(List.of("助餐"), new RedisCacheAspect(redis).around(point, annotation()));
        verify(point).proceed();
    }

    @Test
    void cacheWriteFailureDoesNotRetryDatabaseQuery() throws Throwable {
        RedisUtil redis = redis();
        doThrow(new RedisConnectionFailureException("测试断连")).when(redis)
                .set(anyString(), any(), anyLong(), any(TimeUnit.class));
        ProceedingJoinPoint point = mock(ProceedingJoinPoint.class);
        when(point.proceed()).thenReturn(List.of());
        assertEquals(List.of(), new RedisCacheAspect(redis).around(point, annotation()));
        verify(point).proceed();
    }

    @Test
    void nullValueStillHasShortTtlAndDatabaseFailureIsNotSwallowed() throws Throwable {
        RedisUtil redis = redis();
        ProceedingJoinPoint point = mock(ProceedingJoinPoint.class);
        assertNull(new RedisCacheAspect(redis).around(point, annotation()));
        verify(redis).setNull(eq("test:hot"), longThat(ttl -> ttl >= 108 && ttl <= 132), eq(TimeUnit.SECONDS));
        when(point.proceed()).thenThrow(new IllegalStateException("数据库业务异常"));
        assertThrows(IllegalStateException.class, () -> new RedisCacheAspect(redis).around(point, annotation()));
    }

    @Test
    void invalidPaginationCannotDisableLimit() {
        PageResultDTO page = new PageResultDTO();
        assertThrows(IllegalArgumentException.class, () -> page.setPageNum(0));
        assertThrows(IllegalArgumentException.class, () -> page.setPageSize(-1));
        assertThrows(IllegalArgumentException.class, () -> page.setPageSize(101));
        page.setPageSize(100);
        assertEquals(100, page.getPageSize());
    }

    @Test
    void directoryInvalidationWaitsForOldLoaderThenRemovesOldResult() throws Exception {
        RedisTemplate<String, Object> template = mock(RedisTemplate.class);
        ValueOperations<String, Object> values = mock(ValueOperations.class);
        when(template.opsForValue()).thenReturn(values);
        Map<String, Object> cache = new ConcurrentHashMap<>();
        when(values.get(anyString())).thenAnswer(call -> cache.get(call.getArgument(0)));
        doAnswer(call -> {
            cache.put(call.getArgument(0), call.getArgument(1));
            return null;
        }).when(values).set(anyString(), any(), anyLong(), any(TimeUnit.class));
        when(template.delete(anyString())).thenAnswer(call -> cache.remove(call.getArgument(0)) != null);
        RedisUtil redis = new RedisUtil(template, mock(StringRedisTemplate.class));
        ReflectionTestUtils.setField(redis, "redisEnabled", true);
        assertSame(redis.cacheMonitor("test:hot"), redis.cacheMonitor("null:test:hot"));
        CountDownLatch reading = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        ExecutorService workers = Executors.newFixedThreadPool(2);
        try {
            Future<Object> loader = workers.submit(() -> {
                ProceedingJoinPoint point = mock(ProceedingJoinPoint.class);
                try {
                    when(point.proceed()).thenAnswer(call -> {
                        reading.countDown();
                        assertTrue(release.await(5, TimeUnit.SECONDS));
                        return List.of("修改前目录");
                    });
                    return new RedisCacheAspect(redis).around(point, annotation());
                } catch (Throwable ex) {
                    throw new IllegalStateException(ex);
                }
            });
            assertTrue(reading.await(5, TimeUnit.SECONDS));
            Future<Boolean> eviction = workers.submit(() -> redis.delete("test:hot"));
            assertThrows(TimeoutException.class, () -> eviction.get(50, TimeUnit.MILLISECONDS));
            release.countDown();
            assertEquals(List.of("修改前目录"), loader.get(5, TimeUnit.SECONDS));
            assertTrue(eviction.get(5, TimeUnit.SECONDS));
            assertNull(cache.get("test:hot"));
        } finally {
            release.countDown();
            workers.shutdownNow();
        }
    }

    @Test
    void invalidServiceTypeNeverReachesCacheOrDatabase() {
        var service = mock(com.me.service.ServicePackageService.class);
        var controller = new com.me.controller.user.ServiceController(service);
        assertEquals(400, controller.getServiceList(-1).getCode());
        assertEquals(400, controller.getServiceList(999).getCode());
        verifyNoInteractions(service);
    }

    @Test
    void cachedNullAndEmptyListDoNotHitDatabase() throws Throwable {
        RedisUtil redis = redis();
        ProceedingJoinPoint point = mock(ProceedingJoinPoint.class);
        when(redis.isNullCached("test:hot")).thenReturn(true);
        assertNull(new RedisCacheAspect(redis).around(point, annotation()));
        when(redis.get("test:hot")).thenReturn(List.of());
        assertEquals(List.of(), new RedisCacheAspect(redis).around(point, annotation()));
        verify(point, never()).proceed();
    }
}

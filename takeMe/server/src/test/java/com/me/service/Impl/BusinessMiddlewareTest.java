package com.me.service.Impl;

import com.me.context.BaseContext;
import com.me.controller.user.UserLoginController;
import com.me.dto.LoginDTO;
import com.me.redis.annotation.RateLimit;
import com.me.redis.aspect.RateLimitAspect;
import com.me.redis.utils.RedisUtil;
import com.me.websocket.WebSocketSessionManager;
import jakarta.websocket.Session;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BusinessMiddlewareTest {
    @Test
    void sharedIpDoesNotMergeLoginAccountsAndForwardedHeaderIsUntrusted() throws Throwable {
        RedisUtil redis = mock(RedisUtil.class);
        Map<String, AtomicInteger> attempts = new ConcurrentHashMap<>();
        when(redis.rateLimit(anyString(), anyInt(), anyInt())).thenAnswer(call -> {
            int count = attempts.computeIfAbsent(call.getArgument(0), key -> new AtomicInteger()).incrementAndGet();
            return count <= (int) call.getArgument(2);
        });
        RateLimitAspect aspect = new RateLimitAspect(redis);
        ReflectionTestUtils.setField(aspect, "loginIpCount", 1000);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("X-Forwarded-For", "8.8.8.8");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        ProceedingJoinPoint point = mock(ProceedingJoinPoint.class);
        when(point.proceed()).thenReturn("通过");
        RateLimit limit = UserLoginController.class.getMethod("login", LoginDTO.class).getAnnotation(RateLimit.class);
        try {
            LoginDTO login = new LoginDTO();
            login.setUsername("elder1");
            when(point.getArgs()).thenReturn(new Object[]{login});
            for (int i = 0; i < 10; i++) assertEquals("通过", aspect.around(point, limit));
            assertNotEquals("通过", aspect.around(point, limit));
            login.setUsername("elder2");
            assertEquals("通过", aspect.around(point, limit));
            assertTrue(attempts.containsKey("rate:login:ip:127.0.0.1"));
            assertFalse(attempts.keySet().stream().anyMatch(key -> key.contains("8.8.8.8")));
        } finally {
            BaseContext.clear();
            RequestContextHolder.resetRequestAttributes();
        }
    }

    @Test
    void onlineStatisticsCountOpenConnectionsOnly() {
        WebSocketSessionManager manager = new WebSocketSessionManager();
        Session open = mock(Session.class), closed = mock(Session.class);
        when(open.isOpen()).thenReturn(true);
        manager.addUserSession("middleware-test-user", open);
        manager.addVolunteerSession("middleware-test-volunteer", closed);
        try {
            Map<String, Object> result = new OnlineUserServiceImpl(manager).getOnlineStats();
            assertEquals(1L, result.get("total"));
            assertEquals(0L, result.get("volunteerCount"));
        } finally {
            manager.removeUserSession("middleware-test-user", open);
            manager.removeVolunteerSession("middleware-test-volunteer", closed);
        }
    }
}

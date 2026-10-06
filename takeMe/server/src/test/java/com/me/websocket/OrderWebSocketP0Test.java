package com.me.websocket;

import com.me.util.SpringContextUtil;
import com.me.utils.JwtUtil;
import com.me.service.AccountAccessService;
import jakarta.websocket.Session;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;

class OrderWebSocketP0Test {

    @Test
    void matchingRoleAndIdentityAreRequiredBeforeSubscription() throws Exception {
        Session session = mock(Session.class);
        JwtUtil jwt = mock(JwtUtil.class);
        WebSocketSessionManager manager = mock(WebSocketSessionManager.class);
        AccountAccessService access = mock(AccountAccessService.class);
        when(access.isActive(1L, 2)).thenReturn(true);
        when(session.getRequestParameterMap()).thenReturn(Map.of("token", List.of("valid")));
        when(jwt.getRole("valid")).thenReturn(2);
        when(jwt.getUserId("valid")).thenReturn(1L);

        try (MockedStatic<SpringContextUtil> context = mockStatic(SpringContextUtil.class)) {
            context.when(() -> SpringContextUtil.getBean(JwtUtil.class)).thenReturn(jwt);
            context.when(() -> SpringContextUtil.getBean(WebSocketSessionManager.class)).thenReturn(manager);
            context.when(() -> SpringContextUtil.getBean(AccountAccessService.class)).thenReturn(access);

            new OrderWebSocketEndpoint().onOpen(session, "user", "2");
            verify(session).close();
            verifyNoInteractions(manager);

            OrderWebSocketEndpoint allowed = new OrderWebSocketEndpoint();
            allowed.onOpen(session, "user", "1");
            verify(manager).addUserSession("1", session);
            allowed.onClose(session, "user", "1");
            verify(manager).removeUserSession("1", session);
        }
    }

    @Test
    void disabledAccountCannotSubscribeWithOldToken() throws Exception {
        Session session = mock(Session.class);
        JwtUtil jwt = mock(JwtUtil.class);
        when(session.getRequestParameterMap()).thenReturn(Map.of("token", List.of("old")));
        when(jwt.getRole("old")).thenReturn(2);
        when(jwt.getUserId("old")).thenReturn(1L);
        try (MockedStatic<SpringContextUtil> context = mockStatic(SpringContextUtil.class)) {
            context.when(() -> SpringContextUtil.getBean(JwtUtil.class)).thenReturn(jwt);
            context.when(() -> SpringContextUtil.getBean(AccountAccessService.class))
                    .thenReturn(mock(AccountAccessService.class));
            new OrderWebSocketEndpoint().onOpen(session, "user", "1");
            verify(session).close();
        }
    }
}

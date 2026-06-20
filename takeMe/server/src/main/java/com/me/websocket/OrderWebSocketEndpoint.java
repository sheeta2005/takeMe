package com.me.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.me.dto.OrderStatusChangeWsMessage;
import com.me.dto.WebSocketMessage;
import com.me.util.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.time.LocalDateTime;

@Slf4j
@Component
@ServerEndpoint("/ws/order/{userType}/{userId}")
public class OrderWebSocketEndpoint {

    private static ObjectMapper objectMapper;

    private static ObjectMapper getObjectMapper() {
        if (objectMapper == null) {
            objectMapper = SpringContextUtil.getBean(ObjectMapper.class);
        }
        return objectMapper;
    }

    @OnOpen
    public void onOpen(Session session, @PathParam("userType") String userType, @PathParam("userId") String userId) {
        WebSocketSessionManager sessionManager = SpringContextUtil.getBean(WebSocketSessionManager.class);
        
        switch (userType.toLowerCase()) {
            case "user":
                sessionManager.addUserSession(userId, session);
                break;
            case "volunteer":
                sessionManager.addVolunteerSession(userId, session);
                break;
            case "admin":
                sessionManager.addAdminSession(userId, session);
                break;
            default:
                log.warn("未知的用户类型: {}", userType);
                try {
                    session.close();
                } catch (IOException e) {
                    log.error("关闭WebSocket连接失败", e);
                }
        }
    }

    @OnClose
    public void onClose(@PathParam("userType") String userType, @PathParam("userId") String userId) {
        WebSocketSessionManager sessionManager = SpringContextUtil.getBean(WebSocketSessionManager.class);
        
        switch (userType.toLowerCase()) {
            case "user":
                sessionManager.removeUserSession(userId);
                break;
            case "volunteer":
                sessionManager.removeVolunteerSession(userId);
                break;
            case "admin":
                sessionManager.removeAdminSession(userId);
                break;
        }
    }

    @OnError
    public void onError(Session session, Throwable error, @PathParam("userType") String userType, @PathParam("userId") String userId) {
        log.error("WebSocket错误: userType={}, userId={}", userType, userId, error);

        WebSocketSessionManager sessionManager = SpringContextUtil.getBean(WebSocketSessionManager.class);
        
        switch (userType.toLowerCase()) {
            case "user":
                sessionManager.removeUserSession(userId);
                break;
            case "volunteer":
                sessionManager.removeVolunteerSession(userId);
                break;
            case "admin":
                sessionManager.removeAdminSession(userId);
                break;
        }
    }

    public static void sendMessageToUser(String userId, OrderStatusChangeWsMessage message) {
        try {
            WebSocketMessage wsMessage = WebSocketMessage.builder()
                    .type("ORDER_STATUS_CHANGE")
                    .data(message)
                    .timestamp(LocalDateTime.now())
                    .build();

            String jsonMessage = getObjectMapper().writeValueAsString(wsMessage);

            Session session = SpringContextUtil.getBean(WebSocketSessionManager.class).getUserSession(userId);
            if (session != null && session.isOpen()) {
                session.getBasicRemote().sendText(jsonMessage);
                log.info("WebSocket消息发送成功: userId={}, orderId={}", userId, message.getOrderId());
            } else {
                log.warn("用户WebSocket连接不存在或已关闭: userId={}", userId);
            }
        } catch (Exception e) {
            log.error("发送WebSocket消息失败: userId={}", userId, e);
        }
    }

    public static void sendMessageToVolunteer(String volunteerId, OrderStatusChangeWsMessage message) {
        try {
            WebSocketMessage wsMessage = WebSocketMessage.builder()
                    .type("ORDER_STATUS_CHANGE")
                    .data(message)
                    .timestamp(LocalDateTime.now())
                    .build();

            String jsonMessage = getObjectMapper().writeValueAsString(wsMessage);

            Session session = SpringContextUtil.getBean(WebSocketSessionManager.class).getVolunteerSession(volunteerId);
            if (session != null && session.isOpen()) {
                session.getBasicRemote().sendText(jsonMessage);
                log.info("WebSocket消息发送成功: volunteerId={}, orderId={}", volunteerId, message.getOrderId());
            } else {
                log.warn("志愿者WebSocket连接不存在或已关闭: volunteerId={}", volunteerId);
            }
        } catch (Exception e) {
            log.error("发送WebSocket消息失败: volunteerId={}", volunteerId, e);
        }
    }

    public static void sendMessageToAdmin(String adminId, OrderStatusChangeWsMessage message) {
        try {
            WebSocketMessage wsMessage = WebSocketMessage.builder()
                    .type("ORDER_STATUS_CHANGE")
                    .data(message)
                    .timestamp(LocalDateTime.now())
                    .build();

            String jsonMessage = getObjectMapper().writeValueAsString(wsMessage);

            Session session = SpringContextUtil.getBean(WebSocketSessionManager.class).getAdminSession(adminId);
            if (session != null && session.isOpen()) {
                session.getBasicRemote().sendText(jsonMessage);
                log.info("WebSocket消息发送成功: adminId={}, orderId={}", adminId, message.getOrderId());
            } else {
                log.warn("管理员WebSocket连接不存在或已关闭: adminId={}", adminId);
            }
        } catch (Exception e) {
            log.error("发送WebSocket消息失败: adminId={}", adminId, e);
        }
    }
}

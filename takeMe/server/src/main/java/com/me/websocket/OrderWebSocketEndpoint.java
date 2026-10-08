package com.me.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.me.dto.OrderStatusChangeWsMessage;
import com.me.dto.WebSocketMessage;
import com.me.util.SpringContextUtil;
import com.me.utils.JwtUtil;
import com.me.service.AccountAccessService;
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
    private boolean authenticated;

    //获取消息序列化器
    private static ObjectMapper getObjectMapper() {
        if (objectMapper == null) {
            objectMapper = SpringContextUtil.getBean(ObjectMapper.class);
        }
        return objectMapper;
    }

    //校验身份并登记实时连接
    @OnOpen
    public void onOpen(Session session, @PathParam("userType") String userType, @PathParam("userId") String userId) {
        try {
            String token = session.getRequestParameterMap().getOrDefault("token", java.util.List.of())
                    .stream().findFirst().orElse(null);
            JwtUtil jwt = SpringContextUtil.getBean(JwtUtil.class);
            Integer role = switch (userType.toLowerCase()) {
                case "admin" -> 0;
                case "volunteer" -> 1;
                case "user" -> 2;
                default -> null;
            };
            if (token == null || role == null
                    || !role.equals(jwt.getRole(token))
                    || !userId.equals(String.valueOf(jwt.getUserId(token)))
                    || !SpringContextUtil.getBean(AccountAccessService.class)
                        .isActive(jwt.getUserId(token), role)) {
                session.close();
                return;
            }
        } catch (Exception e) {
            log.warn("WebSocket鉴权失败: userType={}, userId={}", userType, userId);
            try {
                session.close();
            } catch (IOException closeError) {
                log.error("关闭WebSocket连接失败", closeError);
            }
            return;
        }
        authenticated = true;
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

    //移除已断开的实时连接
    @OnClose
    public void onClose(Session session, @PathParam("userType") String userType, @PathParam("userId") String userId) {
        if (!authenticated) return;
        WebSocketSessionManager sessionManager = SpringContextUtil.getBean(WebSocketSessionManager.class);
        
        switch (userType.toLowerCase()) {
            case "user":
                sessionManager.removeUserSession(userId, session);
                break;
            case "volunteer":
                sessionManager.removeVolunteerSession(userId, session);
                break;
            case "admin":
                sessionManager.removeAdminSession(userId, session);
                break;
        }
    }

    //推送新消息提醒
    public static void sendMessageReminder(Integer role, Long receiverId, com.me.entity.Message message) {
        try {
            WebSocketSessionManager manager = SpringContextUtil.getBean(WebSocketSessionManager.class);
            Session session = role == 2 ? manager.getUserSession(receiverId.toString())
                    : manager.getVolunteerSession(receiverId.toString());
            if (session == null || !session.isOpen()) return;
            // 群发落库后只推送提醒，离线用户仍从消息列表获取完整内容。
            WebSocketMessage reminder = WebSocketMessage.builder().type("NEW_MESSAGE")
                    .data(java.util.Map.of("title", message.getTitle(), "content", message.getContent()))
                    .timestamp(LocalDateTime.now()).build();
            session.getAsyncRemote().sendText(getObjectMapper().writeValueAsString(reminder));
        } catch (Exception error) {
            log.debug("消息提醒推送失败，不影响已持久化通知: receiverId={}", receiverId, error);
        }
    }

    //记录连接异常并清理会话
    @OnError
    public void onError(Session session, Throwable error, @PathParam("userType") String userType, @PathParam("userId") String userId) {
        log.error("WebSocket错误: userType={}, userId={}", userType, userId, error);
        onClose(session, userType, userId);
    }

    //向用户推送订单状态
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
                log.debug("用户当前离线，通知已保存在消息列表: userId={}", userId);
            }
        } catch (Exception e) {
            log.error("发送WebSocket消息失败: userId={}", userId, e);
        }
    }

    //向志愿者推送订单状态
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
                log.debug("志愿者当前离线，通知已保存在消息列表: volunteerId={}", volunteerId);
            }
        } catch (Exception e) {
            log.error("发送WebSocket消息失败: volunteerId={}", volunteerId, e);
        }
    }

    //向管理员推送订单状态
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

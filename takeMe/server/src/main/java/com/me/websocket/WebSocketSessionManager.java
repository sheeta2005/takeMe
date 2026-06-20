package com.me.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.websocket.Session;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class WebSocketSessionManager {

    private static final Map<String, Session> userSessions = new ConcurrentHashMap<>();
    private static final Map<String, Session> volunteerSessions = new ConcurrentHashMap<>();
    private static final Map<String, Session> adminSessions = new ConcurrentHashMap<>();

    public void addUserSession(String userId, Session session) {
        userSessions.put(userId, session);
        log.info("用户WebSocket连接建立: userId={}", userId);
    }

    public void addVolunteerSession(String volunteerId, Session session) {
        volunteerSessions.put(volunteerId, session);
        log.info("志愿者WebSocket连接建立: volunteerId={}", volunteerId);
    }

    public void addAdminSession(String adminId, Session session) {
        adminSessions.put(adminId, session);
        log.info("管理员WebSocket连接建立: adminId={}", adminId);
    }

    public void removeUserSession(String userId) {
        userSessions.remove(userId);
        log.info("用户WebSocket连接断开: userId={}", userId);
    }

    public void removeVolunteerSession(String volunteerId) {
        volunteerSessions.remove(volunteerId);
        log.info("志愿者WebSocket连接断开: volunteerId={}", volunteerId);
    }

    public void removeAdminSession(String adminId) {
        adminSessions.remove(adminId);
        log.info("管理员WebSocket连接断开: adminId={}", adminId);
    }

    public Session getUserSession(String userId) {
        return userSessions.get(userId);
    }

    public Session getVolunteerSession(String volunteerId) {
        return volunteerSessions.get(volunteerId);
    }

    public Session getAdminSession(String adminId) {
        return adminSessions.get(adminId);
    }

    public Map<String, Session> getAllUserSessions() {
        return userSessions;
    }

    public Map<String, Session> getAllVolunteerSessions() {
        return volunteerSessions;
    }

    public Map<String, Session> getAllAdminSessions() {
        return adminSessions;
    }
}

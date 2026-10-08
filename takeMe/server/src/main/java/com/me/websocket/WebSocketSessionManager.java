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

    //登记用户实时连接
    public void addUserSession(String userId, Session session) {
        userSessions.put(userId, session);
        log.info("用户WebSocket连接建立: userId={}", userId);
    }

    //登记志愿者实时连接
    public void addVolunteerSession(String volunteerId, Session session) {
        volunteerSessions.put(volunteerId, session);
        log.info("志愿者WebSocket连接建立: volunteerId={}", volunteerId);
    }

    //登记管理员实时连接
    public void addAdminSession(String adminId, Session session) {
        adminSessions.put(adminId, session);
        log.info("管理员WebSocket连接建立: adminId={}", adminId);
    }

    //移除用户实时连接
    public void removeUserSession(String userId, Session session) {
        userSessions.remove(userId, session);
        log.info("用户WebSocket连接断开: userId={}", userId);
    }

    //移除志愿者实时连接
    public void removeVolunteerSession(String volunteerId, Session session) {
        volunteerSessions.remove(volunteerId, session);
        log.info("志愿者WebSocket连接断开: volunteerId={}", volunteerId);
    }

    //移除管理员实时连接
    public void removeAdminSession(String adminId, Session session) {
        adminSessions.remove(adminId, session);
        log.info("管理员WebSocket连接断开: adminId={}", adminId);
    }

    //获取用户实时连接
    public Session getUserSession(String userId) {
        return userSessions.get(userId);
    }

    //获取志愿者实时连接
    public Session getVolunteerSession(String volunteerId) {
        return volunteerSessions.get(volunteerId);
    }

    //获取管理员实时连接
    public Session getAdminSession(String adminId) {
        return adminSessions.get(adminId);
    }

    //获取全部用户连接
    public Map<String, Session> getAllUserSessions() {
        return userSessions;
    }

    //获取全部志愿者连接
    public Map<String, Session> getAllVolunteerSessions() {
        return volunteerSessions;
    }

    //获取全部管理员连接
    public Map<String, Session> getAllAdminSessions() {
        return adminSessions;
    }
}

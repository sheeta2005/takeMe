package com.me.service.Impl;

import com.me.websocket.WebSocketSessionManager;
import jakarta.websocket.Session;
import com.me.service.OnlineUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class OnlineUserServiceImpl implements OnlineUserService {

    private final WebSocketSessionManager sessionManager;

    //统计各角色在线人数
    @Override
    public Map<String, Object> getOnlineStats() {
        // 单 JVM 部署复用实时连接，不将一天内登录误算为在线。
        long adminCount = count(sessionManager.getAllAdminSessions());
        long volunteerCount = count(sessionManager.getAllVolunteerSessions());
        long userCount = count(sessionManager.getAllUserSessions());
        return Map.of("total", adminCount + volunteerCount + userCount,
                "adminCount", adminCount, "volunteerCount", volunteerCount, "userCount", userCount);
    }

    //统计有效连接数量
    private long count(Map<String, Session> sessions) {
        return sessions.values().stream().filter(Session::isOpen).count();
    }
}

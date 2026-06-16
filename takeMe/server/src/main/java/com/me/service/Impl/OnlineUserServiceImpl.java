package com.me.service.Impl;

import com.me.redis.utils.RedisUtil;
import com.me.service.OnlineUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class OnlineUserServiceImpl implements OnlineUserService {

    private final RedisUtil redisUtil;

    private static final String ONLINE_TOTAL_KEY = "online:total";
    private static final String ONLINE_USER_PREFIX = "online:user:";

    @Override
    public void userOnline(Long userId, Integer role) {
        String userKey = ONLINE_USER_PREFIX + role + ":" + userId;
        // setIfAbsent：key不存在才写入，返回true代表首次登录
        Boolean isNew = redisUtil.setIfAbsent(userKey, "1", 24, TimeUnit.HOURS);
        if (Boolean.TRUE.equals(isNew)) {
            // 新上线用户，在线总数+1
            redisUtil.increment(ONLINE_TOTAL_KEY, 1);
            log.info("用户上线: userId={}, role={}, userKey={}", userId, role, userKey);
        } else {
            // 重复登录，仅刷新过期时间，不重复累加在线人数
            redisUtil.expire(userKey, 24, TimeUnit.HOURS);
            log.debug("用户重复登录，刷新过期时间: userId={}, role={}", userId, role);
        }
    }

    @Override
    public void userOffline(Long userId, Integer role) {
        String userKey = ONLINE_USER_PREFIX + role + ":" + userId;
        if (Boolean.TRUE.equals(redisUtil.hasKey(userKey))) {
            redisUtil.delete(userKey);
            redisUtil.decrement(ONLINE_TOTAL_KEY, 1);
            log.info("用户下线: userId={}, role={}", userId, role);
        }
    }

    @Override
    public Map<String, Object> getOnlineStats() {
        Set<String> allOnlineKeys = redisUtil.keys(ONLINE_USER_PREFIX + "*");

        int total = allOnlineKeys != null ? allOnlineKeys.size() : 0;
        int adminCount = 0;
        int volunteerCount = 0;
        int userCount = 0;

        if (allOnlineKeys != null) {
            for (String key : allOnlineKeys) {
                String[] parts = key.substring(ONLINE_USER_PREFIX.length()).split(":");
                if (parts.length >= 1) {
                    switch (parts[0]) {
                        case "0": adminCount++; break;
                        case "1": volunteerCount++; break;
                        case "2": userCount++; break;
                    }
                }
            }
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", total);
        stats.put("adminCount", adminCount);
        stats.put("volunteerCount", volunteerCount);
        stats.put("userCount", userCount);
        return stats;
    }
}
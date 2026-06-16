package com.me.service.task;

import com.me.redis.utils.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class OnlineUserCalibrationTask {

    private final RedisUtil redisUtil;

    private static final String ONLINE_TOTAL_KEY = "online:total";
    private static final String ONLINE_USER_PREFIX = "online:user:";

    @Scheduled(fixedRate = 60000)
    public void calibrate() {
        Set<String> allOnlineKeys = redisUtil.keys(ONLINE_USER_PREFIX + "*");
        int actualCount = allOnlineKeys != null ? allOnlineKeys.size() : 0;

        Object currentCount = redisUtil.get(ONLINE_TOTAL_KEY);
        int expectedCount = currentCount != null ? Integer.parseInt(currentCount.toString()) : 0;

        if (actualCount != expectedCount) {
            log.warn("在线计数校准: INCR计数={}, 实际存活key数={}, 修正为{}", expectedCount, actualCount, actualCount);
            redisUtil.set(ONLINE_TOTAL_KEY, actualCount);
        } else {
            log.debug("在线计数校验通过: count={}", actualCount);
        }
    }
}

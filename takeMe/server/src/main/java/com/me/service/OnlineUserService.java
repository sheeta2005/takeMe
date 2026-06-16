package com.me.service;

import java.util.Map;

public interface OnlineUserService {

    void userOnline(Long userId, Integer role);

    void userOffline(Long userId, Integer role);

    Map<String, Object> getOnlineStats();
}

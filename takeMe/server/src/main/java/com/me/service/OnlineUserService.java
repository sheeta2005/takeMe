package com.me.service;

import java.util.Map;

public interface OnlineUserService {

    //统计各角色在线人数
    Map<String, Object> getOnlineStats();
}

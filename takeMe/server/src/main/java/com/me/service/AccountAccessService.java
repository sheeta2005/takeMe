package com.me.service;

import com.me.mapper.AdminMapper;
import com.me.mapper.UserMapper;
import com.me.mapper.VolunteerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountAccessService {
    private final UserMapper userMapper;
    private final VolunteerMapper volunteerMapper;
    private final AdminMapper adminMapper;

    // JWT 签名有效不代表账号仍可用，HTTP 与 WebSocket 共用实时状态检查。
    public boolean isActive(Long id, Integer role) {
        if (id == null || role == null) return false;
        return switch (role) {
            case 0 -> adminMapper.selectById(id) != null;
            case 1 -> volunteerMapper.countActiveAccount(id) == 1;
            case 2 -> userMapper.countActiveAccount(id) == 1;
            default -> false;
        };
    }
}

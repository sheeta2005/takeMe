package com.me.controller.admin;

import com.me.dto.LoginDTO;
import com.me.entity.Admin;
import com.me.redis.annotation.RateLimit;
import com.me.result.Result;
import com.me.service.AdminService;
import com.me.service.OnlineUserService;
import com.me.utils.JwtUtil;
import com.me.vo.LoginVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理员-登录接口")
@RestController
@RequestMapping("/api/admin")
public class AdminLoginController {

    private final AdminService adminService;
    private final JwtUtil jwtUtil;
    private final OnlineUserService onlineUserService;


    public AdminLoginController(AdminService adminService, JwtUtil jwtUtil, OnlineUserService onlineUserService) {
        this.adminService = adminService;
        this.jwtUtil = jwtUtil;
        this.onlineUserService = onlineUserService;
    }

    @Operation(summary = "登录")
    @RateLimit(prefix = "rate:admin:login", count = 10, period = 60)
    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody LoginDTO loginDTO) {
        Admin admin = adminService.login(loginDTO);
        if (admin == null) {
            return Result.error("账号或密码错误");
        }
        onlineUserService.userOnline(admin.getId(), 0);
        LoginVO loginVO = jwtUtil.buildLoginVO(admin.getId(), 0, admin.getRealName(), null);
        return Result.success(loginVO);
    }

    @Operation(summary = "登出")
    @PostMapping("/logout")
    public Result<Void> logout() {
        Long userId = com.me.context.BaseContext.getLoginId();
        Integer role = com.me.context.BaseContext.getLoginType();
        if (userId != null) {
            onlineUserService.userOffline(userId, role);
        }
        return Result.success();
    }
}
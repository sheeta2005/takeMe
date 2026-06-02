package com.me.controller.admin;

import com.me.dto.LoginDTO;
import com.me.entity.Admin;
import com.me.result.Result;
import com.me.service.AdminService;
import com.me.utils.JwtUtil;
import com.me.vo.LoginVO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminLoginController {

    private final AdminService adminService;
    private final JwtUtil jwtUtil;

    public AdminLoginController(AdminService adminService, JwtUtil jwtUtil) {
        this.adminService = adminService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody LoginDTO loginDTO) {
        Admin admin = adminService.login(loginDTO);
        if (admin == null) {
            return Result.error("账号或密码错误");
        }
        LoginVO loginVO = jwtUtil.buildLoginVO(admin.getId(), 0, admin.getRealName(), null);
        return Result.success(loginVO);
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.success();
    }
}
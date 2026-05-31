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

    /**
     * 管理员登录接口
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody LoginDTO loginDTO) {
        Admin admin = adminService.login(loginDTO);
        if (admin == null) {
            return Result.error("账号或密码错误");
        }
        // 调用JwtUtil的通用方法构建返回结果
        LoginVO loginVO = jwtUtil.buildLoginVO(admin.getId(), 0, admin.getRealName(), null);
        return Result.success(loginVO);
    }
}
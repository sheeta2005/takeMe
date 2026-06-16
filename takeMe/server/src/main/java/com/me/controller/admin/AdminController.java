package com.me.controller.admin;

import com.me.context.BaseContext;
import com.me.entity.Admin;
import com.me.result.Result;
import com.me.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    //获取管理员信息
    @GetMapping("/info")
    public Result<Admin> getAdminInfo() {
        Long adminId = BaseContext.getLoginId();
        Admin admin = adminService.getAdminInfo(adminId);

        if (admin == null) {
            return Result.error("管理员不存在");
        }

        return Result.success(admin);
    }

    //更新管理员信息
    @PostMapping("/update")
    public Result<Void> updateAdminInfo(@RequestBody Admin admin) {
        Long adminId = BaseContext.getLoginId();
        admin.setId(adminId);

        boolean success = adminService.updateAdminInfo(admin);

        if (success) {
            return Result.success();
        } else {
            return Result.error("更新失败");
        }
    }
}

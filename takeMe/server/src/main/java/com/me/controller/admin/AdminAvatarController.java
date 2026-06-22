package com.me.controller.admin;

import com.me.context.BaseContext;
import com.me.result.Result;
import com.me.service.AdminService;
import com.me.util.OssUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/admin/avatar")
@RequiredArgsConstructor
@Tag(name = "管理员-头像管理", description = "管理员头像上传、更新接口")
public class AdminAvatarController {

    private final AdminService adminService;
    private final OssUtil ossUtil;

    @PostMapping("/upload")
    @Operation(summary = "上传管理员头像")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        Long adminId = BaseContext.getLoginId();

        byte[] fileContent;
        try {
            fileContent = file.getBytes();
        } catch (Exception e) {
            log.error("Failed to read file content", e);
            return Result.error("文件读取失败");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            return Result.error("文件名不能为空");
        }

        String avatarUrl = ossUtil.uploadFile(fileContent, originalFilename, "admin");

        adminService.updateAvatar(adminId, avatarUrl);

        log.info("Admin {} uploaded avatar: {}", adminId, avatarUrl);
        return Result.success(avatarUrl);
    }

    @DeleteMapping
    @Operation(summary = "删除管理员头像")
    public Result<Void> deleteAvatar() {
        Long adminId = BaseContext.getLoginId();
        adminService.deleteAvatar(adminId);
        return Result.success();
    }
}

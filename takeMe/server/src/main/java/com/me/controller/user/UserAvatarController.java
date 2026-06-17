package com.me.controller.user;

import com.me.context.BaseContext;
import com.me.dto.AvatarUploadDTO;
import com.me.result.Result;
import com.me.service.UserService;
import com.me.util.OssUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/user/avatar")
@RequiredArgsConstructor
@Tag(name = "用户头像管理", description = "用户头像上传、更新接口")
public class UserAvatarController {

    private final UserService userService;
    private final OssUtil ossUtil;

    @PostMapping("/upload")
    @Operation(summary = "上传用户头像")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        Long userId = BaseContext.getLoginId();

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

        String avatarUrl = ossUtil.uploadFile(fileContent, originalFilename, "user");

        userService.updateAvatar(userId, avatarUrl);

        log.info("User {} uploaded avatar: {}", userId, avatarUrl);
        return Result.success(avatarUrl);
    }

    @DeleteMapping
    @Operation(summary = "删除用户头像")
    public Result<Void> deleteAvatar() {
        Long userId = BaseContext.getLoginId();
        userService.deleteAvatar(userId);
        return Result.success();
    }
}

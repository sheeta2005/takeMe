package com.me.controller.volunteer;

import com.me.context.BaseContext;
import com.me.result.Result;
import com.me.service.VolunteerService;
import com.me.util.OssUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/volunteer/avatar")
@RequiredArgsConstructor
@Tag(name = "志愿者-头像管理", description = "志愿者头像上传、更新接口")
public class VolunteerAvatarController {

    private final VolunteerService volunteerService;
    private final OssUtil ossUtil;

    @PostMapping("/upload")
    @Operation(summary = "上传志愿者头像")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        Long volunteerId = BaseContext.getLoginId();

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

        String avatarUrl = ossUtil.uploadFile(fileContent, originalFilename, "volunteer");

        volunteerService.updateAvatar(volunteerId, avatarUrl);

        log.info("Volunteer {} uploaded avatar: {}", volunteerId, avatarUrl);
        return Result.success(avatarUrl);
    }

    @DeleteMapping
    @Operation(summary = "删除志愿者头像")
    public Result<Void> deleteAvatar() {
        Long volunteerId = BaseContext.getLoginId();
        volunteerService.deleteAvatar(volunteerId);
        return Result.success();
    }
}

package com.me.controller.volunteer;

import com.me.context.BaseContext;
import com.me.entity.Volunteer;
import com.me.result.Result;
import com.me.service.VolunteerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/volunteer")
@RequiredArgsConstructor
public class VolunteerController {

    private final VolunteerService volunteerService;

    /**
     * 获取当前登录志愿者信息
     */
    @GetMapping("/info")
    public Result<Volunteer> getInfo() {
        Long volunteerId = BaseContext.getLoginId();
        Volunteer volunteer = volunteerService.getById(volunteerId);
        
        if (volunteer == null) {
            return Result.error("志愿者不存在");
        }
        
        volunteer.setPassword(null);
        return Result.success(volunteer);
    }

    /**
     * 修改志愿者个人信息
     */
    @PostMapping("/update")
    public Result<Void> update(@RequestBody Volunteer volunteer) {
        Long volunteerId = BaseContext.getLoginId();
        
        volunteer.setId(volunteerId);
        volunteer.setUsername(null);
        volunteer.setPassword(null);
        volunteer.setStatus(null);
        volunteer.setCreateTime(null);
        volunteer.setLastLoginTime(null);
        volunteer.setWorkStatus(null);
        volunteer.setTotalServiceHours(null);
        
        volunteerService.updateById(volunteer);
        return Result.success();
    }

    /**
     * 头像上传
     */
    @PostMapping("/uploadAvatar")
    public Result<Map<String, String>> uploadAvatar(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam("file") MultipartFile file
    ) {
        if (file.isEmpty()) {
            return Result.error("上传文件不能为空");
        }

        try {
            String originalFilename = file.getOriginalFilename();
            String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
            String fileName = UUID.randomUUID() + suffix;

            String uploadPath = "D:/uploads/volunteer-avatar/";
            File dest = new File(uploadPath + fileName);
            if (!dest.getParentFile().exists()) {
                dest.getParentFile().mkdirs();
            }
            file.transferTo(dest);

            Long volunteerId = BaseContext.getLoginId();
            Volunteer volunteer = new Volunteer();
            volunteer.setId(volunteerId);
            String avatarUrl = "http://localhost:8080/uploads/volunteer-avatar/" + fileName;
            volunteer.setAvatar(avatarUrl);
            volunteerService.updateById(volunteer);

            Map<String, String> data = new HashMap<>();
            data.put("url", avatarUrl);
            return Result.success(data);
        } catch (IOException e) {
            e.printStackTrace();
            return Result.error("头像上传失败");
        }
    }
}
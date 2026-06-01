package com.me.controller.user;

import com.me.entity.User;
import com.me.result.Result;
import com.me.service.UserService;
import com.me.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor // ✅ 自动生成构造器
public class UserController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    // 获取当前用户信息
    @GetMapping("/info")
    public Result<User> getUserInfo(@RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserId(token);
        User user = userService.getById(userId);

        if (user == null) {
            return Result.error("用户不存在");
        }

        user.setPassword(null);
        return Result.success(user);
    }

    // 修改用户信息
    @PostMapping("/update")
    public Result updateUserInfo(
            @RequestHeader("Authorization") String token,
            @RequestBody User user
    ) {
        Long userId = jwtUtil.getUserId(token);
        user.setId(userId);

        // 禁止修改敏感字段
        user.setUsername(null);
        user.setPassword(null);
        user.setStatus(null);
        user.setCreateTime(null);
        user.setLastLoginTime(null);

        userService.updateById(user);
        return Result.success();
    }

    // 头像上传
    @PostMapping("/uploadAvatar")
    public Result<Map<String, String>> uploadAvatar(
            @RequestHeader("Authorization") String token,
            @RequestParam("file") MultipartFile file
    ) {
        if (file.isEmpty()) {
            return Result.error("上传文件不能为空");
        }

        try {
            String originalFilename = file.getOriginalFilename();
            String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
            String fileName = UUID.randomUUID() + suffix;

            // ⚠️ 替换成你服务器的实际上传路径
            String uploadPath = "D:/uploads/avatar/";
            File dest = new File(uploadPath + fileName);
            if (!dest.getParentFile().exists()) {
                dest.getParentFile().mkdirs();
            }
            file.transferTo(dest);

            Long userId = jwtUtil.getUserId(token);
            User user = new User();
            user.setId(userId);
            // ⚠️ 替换成你实际的访问前缀
            String avatarUrl = "http://localhost:8080/uploads/avatar/" + fileName;
            user.setAvatar(avatarUrl);
            userService.updateById(user);

            Map<String, String> data = new HashMap<>();
            data.put("url", avatarUrl);
            return Result.success(data);
        } catch (IOException e) {
            e.printStackTrace();
            return Result.error("头像上传失败");
        }
    }


}
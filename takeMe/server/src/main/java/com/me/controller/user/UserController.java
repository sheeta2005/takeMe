package com.me.controller.user;

import com.me.dto.LoginDTO;
import com.me.entity.User;
import com.me.service.UserService;
import com.me.vo.LoginVO;
import com.me.vo.ResultVO;
import com.me.vo.UserInfoVO;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;
    
    // 获取当前登录老人信息
    @GetMapping("/info")
    public ResultVO info(HttpSession session) {
        User user = (User) session.getAttribute("loginUser");
        return ResultVO.success(user);
    }

    // 修改老人信息
    @PostMapping("/update")
    public ResultVO update(@RequestBody User user) {
        userService.updateById(user);
        return ResultVO.success();
    }

    // 头像上传
    @PostMapping("/uploadAvatar")
    public ResultVO uploadAvatar(MultipartFile file) {
        // 这里写你的文件上传逻辑
        return ResultVO.success("上传后的头像地址");
    }
    @PostMapping("/login")
    public ResultVO<LoginVO> login(@RequestBody LoginDTO loginDTO) {
        try {
            return ResultVO.success(userService.login(loginDTO));
        } catch (Exception e) {
            return ResultVO.error(e.getMessage());
        }
    }

    @GetMapping("/info/{userId}")
    public ResultVO<UserInfoVO> getUserInfo(@PathVariable Long userId) {
        try {
            return ResultVO.success(userService.getUserInfo(userId));
        } catch (Exception e) {
            return ResultVO.error(e.getMessage());
        }
    }
}
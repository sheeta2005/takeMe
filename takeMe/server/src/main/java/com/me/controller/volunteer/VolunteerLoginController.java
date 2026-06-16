package com.me.controller.volunteer;

import com.me.dto.LoginDTO;
import com.me.dto.UserRegisterDTO;
import com.me.entity.Volunteer;
import com.me.redis.annotation.RateLimit;
import com.me.result.Result;
import com.me.service.OnlineUserService;
import com.me.service.VolunteerService;
import com.me.utils.JwtUtil;
import com.me.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/volunteer")
@RequiredArgsConstructor
public class VolunteerLoginController {

    private final VolunteerService volunteerService;
    private final JwtUtil jwtUtil;
    private final OnlineUserService onlineUserService;

    @RateLimit(prefix = "rate:volunteer:login", count = 10, period = 60)
    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody LoginDTO loginDTO) {
        Volunteer volunteer = volunteerService.login(loginDTO);
        if (volunteer == null) {
            return Result.error("账号或密码错误");
        }
        onlineUserService.userOnline(volunteer.getId(), 1);
        LoginVO loginVO = jwtUtil.buildLoginVO(
                volunteer.getId(),
                1,
                volunteer.getRealName(),
                volunteer.getAvatar()
        );
        return Result.success(loginVO);
    }

    @PostMapping("/register")
    public Result<Void> register(@RequestBody UserRegisterDTO registerDTO) {
        boolean success = volunteerService.register(registerDTO);
        if (!success) {
            return Result.error("账号已存在");
        }
        return Result.success();
    }

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
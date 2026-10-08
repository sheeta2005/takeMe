package com.me.controller.volunteer;

import com.me.dto.LoginDTO;
import com.me.dto.UserRegisterDTO;
import com.me.entity.Volunteer;
import com.me.redis.annotation.RateLimit;
import com.me.result.Result;
import com.me.service.VolunteerService;
import com.me.utils.JwtUtil;
import com.me.vo.LoginVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "志愿者-登录接口")
@RestController
@RequestMapping("/api/volunteer")
@RequiredArgsConstructor
public class VolunteerLoginController {

    private final VolunteerService volunteerService;
    private final JwtUtil jwtUtil;

    //志愿者登录
    @Operation(summary = "登录")
    @RateLimit(prefix = "rate:volunteer:login", count = 10, period = 60)
    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody LoginDTO loginDTO) {
        Volunteer volunteer = volunteerService.login(loginDTO);
        if (volunteer == null) {
            return Result.error("账号或密码错误");
        }
        LoginVO loginVO = jwtUtil.buildLoginVO(
                volunteer.getId(),
                1,
                volunteer.getRealName(),
                volunteer.getAvatar()
        );
        return Result.success(loginVO);
    }

    //志愿者注册
    @Operation(summary = "注册")
    @PostMapping("/register")
    public Result<Void> register(@RequestBody UserRegisterDTO registerDTO) {
        boolean success = volunteerService.register(registerDTO);
        if (!success) {
            return Result.error("账号已存在");
        }
        return Result.success();
    }

    //志愿者退出登录
    @Operation(summary = "登出")
    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.success();
    }
}

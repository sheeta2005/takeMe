package com.me.controller.volunteer;

import com.me.dto.LoginDTO;
import com.me.dto.UserRegisterDTO;
import com.me.entity.Volunteer;
import com.me.redis.annotation.RateLimit;
import com.me.result.Result;
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

    @RateLimit(prefix = "rate:volunteer:login", count = 10, period = 60)
    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody LoginDTO loginDTO) {
        Volunteer volunteer = volunteerService.login(loginDTO);
        if (volunteer == null) {
            return Result.error("账号或密码错误");
        }
        // 1 = 志愿者
        LoginVO loginVO = jwtUtil.buildLoginVO(
                volunteer.getId(),
                1,
                volunteer.getRealName(),
                volunteer.getAvatar()
        );
        return Result.success(loginVO);
    }

    /**
     * 志愿者注册接口
     */
    @PostMapping("/register")
    public Result<Void> register(@RequestBody UserRegisterDTO registerDTO) {
        boolean success = volunteerService.register(registerDTO);
        if (!success) {
            return Result.error("账号已存在");
        }
        return Result.success();
    }

    /**
     * 退出登录（前端删 token 即可，后端无需复杂逻辑）
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.success();
    }
}
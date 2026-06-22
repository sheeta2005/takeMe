package com.me.controller.user;

import com.me.dto.LoginDTO;
import com.me.dto.PasswordUpdateDTO;
import com.me.dto.UserRegisterDTO;
import com.me.entity.User;
import com.me.redis.annotation.RateLimit;
import com.me.result.Result;
import com.me.service.OnlineUserService;
import com.me.service.UserService;
import com.me.utils.JwtUtil;
import com.me.vo.LoginVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@Tag(name = "用户-登录接口")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserLoginController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final OnlineUserService onlineUserService;

    @Operation(summary = "登入")
    @RateLimit(prefix = "rate:user:login", count = 10, period = 60)
    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody LoginDTO loginDTO) {
        User user = userService.login(loginDTO);
        if (user == null) {
            return Result.error("账号或密码错误");
        }
        onlineUserService.userOnline(user.getId(), 2);
        LoginVO loginVO = jwtUtil.buildLoginVO(
                user.getId(),
                2,
                user.getRealName(),
                user.getAvatar()
        );
        return Result.success(loginVO);
    }

    @Operation(summary = "注册")
    @PostMapping("/register")
    public Result<Void> register(@RequestBody UserRegisterDTO registerDTO) {
        boolean success = userService.register(registerDTO);
        if (!success) {
            return Result.error("账号已存在");
        }
        return Result.success();
    }

    @Operation(summary = "更新密码")
    @PostMapping("/updatePassword")
    public Result<Void> updatePassword(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody PasswordUpdateDTO passwordUpdateDTO
    ) {
        Long userId = jwtUtil.getUserIdFromAuthHeader(authHeader);
        boolean success = userService.updatePassword(
                userId, 
                passwordUpdateDTO.getOldPassword(), 
                passwordUpdateDTO.getNewPassword()
        );
        if (!success) {
            return Result.error("原密码错误");
        }
        return Result.success();
    }

    @Operation(summary = "登出")
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
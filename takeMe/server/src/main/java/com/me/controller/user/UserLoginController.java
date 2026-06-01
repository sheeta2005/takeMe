package com.me.controller.user;

import com.me.dto.LoginDTO;
import com.me.entity.User;
import com.me.result.Result;
import com.me.service.UserService;
import com.me.utils.JwtUtil;
import com.me.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserLoginController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    /**
     * 普通用户登录接口
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody LoginDTO loginDTO) {
        User user = userService.login(loginDTO);
        if (user == null) {
            return Result.error("账号或密码错误");
        }
        // 2 = 普通用户
        LoginVO loginVO = jwtUtil.buildLoginVO(
                user.getId(),
                2,
                user.getRealName(),
                user.getAvatar()
        );
        return Result.success(loginVO);
    }

    /**
     * 退出登录（补你前端调用的接口）
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        // 前端删 token 即可，后端无需复杂逻辑
        return Result.success();
    }
}
package com.me.controller.user;

import com.me.dto.LoginDTO;
import com.me.entity.User;
import com.me.result.Result;
import com.me.service.UserService;
import com.me.utils.JwtUtil;
import com.me.vo.LoginVO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class UserLoginController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    public UserLoginController(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    /**
     * 普通用户登录接口
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody LoginDTO loginDTO) {
        User user = userService.login(loginDTO);
        if (user == null) {
            return Result.error("账号或密码错误");
        }
        LoginVO loginVO = jwtUtil.buildLoginVO(user.getId(), 2, user.getRealName(), user.getAvatar());
        return Result.success(loginVO);
    }
}
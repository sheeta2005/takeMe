package com.me.controller.volunteer;

import com.me.dto.LoginDTO;
import com.me.entity.Volunteer;
import com.me.result.Result;
import com.me.service.VolunteerService;
import com.me.utils.JwtUtil;
import com.me.vo.LoginVO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/volunteer")
public class VolunteerLoginController {

    private final VolunteerService volunteerService;
    private final JwtUtil jwtUtil;

    public VolunteerLoginController(VolunteerService volunteerService, JwtUtil jwtUtil) {
        this.volunteerService = volunteerService;
        this.jwtUtil = jwtUtil;
    }

    /**
     * 志愿者登录接口
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody LoginDTO loginDTO) {
        Volunteer volunteer = volunteerService.login(loginDTO);
        if (volunteer == null) {
            return Result.error("账号或密码错误");
        }
        LoginVO loginVO = jwtUtil.buildLoginVO(volunteer.getId(), 1, volunteer.getRealName(), volunteer.getAvatar());
        return Result.success(loginVO);
    }
}
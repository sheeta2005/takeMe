package com.me.controller;

import com.me.dto.LoginDTO;
import com.me.entity.Admin;
import com.me.entity.User;
import com.me.entity.Volunteer;
import com.me.service.AdminService;
import com.me.service.UserService;
import com.me.service.VolunteerService;
import com.me.utils.JwtUtil;
import com.me.vo.LoginVO;
import com.me.vo.ResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "登录管理", description = "登录相关接口")
@RestController
@RequestMapping("/api")
public class LoginController {

    @Autowired
    private UserService userService;

    @Autowired
    private AdminService adminService;

    @Autowired
    private VolunteerService volunteerService;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Value("${takeMe.jwt.secret-key}")
    private String secretKey;

    @Value("${takeMe.jwt.ttl}")
    private Long ttl;

    @Operation(summary = "登录方法", description = "基于jwt令牌的登录")
    @PostMapping("/login")
    public ResultVO<LoginVO> login(@RequestBody LoginDTO loginDTO) {
        String account = loginDTO.getAccount();
        String password = loginDTO.getPassword();
        Integer role = loginDTO.getRole();

        Long userId = null;
        String username = null;

        // 根据身份校验用户
        if ("admin".equals(role)) {
            Admin admin = adminService.getById(account);
            if (admin == null) {
                return ResultVO.error("账号不存在");
            }
            if (!passwordEncoder.matches(password, admin.getPassword())) {
                return ResultVO.error("密码错误");
            }
            userId = admin.getId();
            username = admin.getName();
        } else if ("elder".equals(role)) {
            User user = userService.getByAccount(account);
            if (user == null) {
                return ResultVO.error("账号不存在");
            }
            if (!passwordEncoder.matches(password, user.getPassword())) {
                return ResultVO.error("密码错误");
            }
            userId = user.getId();
            username = user.getUsername();
        } else if ("volunteer".equals(role)) {
            Volunteer volunteer = volunteerService.getById(account);
            if (volunteer == null) {
                return ResultVO.error("账号不存在");
            }
            if (!passwordEncoder.matches(password, volunteer.getPassword())) {
                return ResultVO.error("密码错误");
            }
            userId = volunteer.getId();
            username = volunteer.getUsername();
        } else {
            return ResultVO.error("身份类型错误");
        }

        // 生成JWT，存入用户ID和身份
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("role", role);
        String token = JwtUtil.createJWT(secretKey, ttl, claims);

        LoginVO loginVO = new LoginVO(userId, username, role, token);
        return ResultVO.success(loginVO);
    }
}
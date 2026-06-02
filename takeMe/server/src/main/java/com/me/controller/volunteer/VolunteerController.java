package com.me.controller.volunteer;

import com.me.context.BaseContext;
import com.me.entity.Volunteer;
import com.me.result.Result;
import com.me.service.VolunteerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/volunteer")
@RequiredArgsConstructor
public class VolunteerController {

    private final VolunteerService volunteerService;

    /**
     * 获取当前登录志愿者信息
     */
    @GetMapping("/info")
    public Result<Volunteer> getInfo() {
        Long volunteerId = BaseContext.getLoginId();
        Volunteer volunteer = volunteerService.getById(volunteerId);
        // 密码脱敏
        volunteer.setPassword(null);
        return Result.success(volunteer);
    }

    /**
     * 修改志愿者个人信息
     */
    @PostMapping("/update")
    public Result<Void> update(@RequestBody Volunteer volunteer) {
        Long volunteerId = BaseContext.getLoginId();
        volunteer.setId(volunteerId);
        volunteerService.updateById(volunteer);
        return Result.success();
    }
}
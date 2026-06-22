package com.me.controller.volunteer;

import com.me.context.BaseContext;
import com.me.entity.VolunteerLeave;
import com.me.result.Result;
import com.me.service.VolunteerLeaveService;
import com.me.vo.VolunteerLeaveVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "志愿者-请假管理")
@RestController
@RequestMapping("/api/volunteer/leave")
@RequiredArgsConstructor
public class VolunteerLeaveController {

    private final VolunteerLeaveService volunteerLeaveService;

    @Operation(summary = "查询假条")
    @GetMapping("/list")
    public Result<List<VolunteerLeaveVO>> list() {
        Long volunteerId = BaseContext.getLoginId();
        List<VolunteerLeaveVO> list = volunteerLeaveService.getListByVolunteerId(volunteerId);
        return Result.success(list);
    }

    @Operation(summary = "请假")
    @PostMapping("/submit")
    public Result<Void> submit(@RequestBody VolunteerLeave leave) {
        try {
            Long volunteerId = BaseContext.getLoginId();
            leave.setVolunteerId(volunteerId);
            volunteerLeaveService.submit(leave);
            return Result.success();
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("提交失败：" + e.getMessage());
        }
    }
}

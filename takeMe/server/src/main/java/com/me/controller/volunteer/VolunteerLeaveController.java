
package com.me.controller.volunteer;

import com.me.context.BaseContext;
import com.me.entity.VolunteerLeave;
import com.me.result.Result;
import com.me.service.VolunteerLeaveService;
import com.me.vo.VolunteerLeaveVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/volunteer/leave")
@RequiredArgsConstructor
public class VolunteerLeaveController {

    private final VolunteerLeaveService volunteerLeaveService;

    @GetMapping("/list")
    public Result<List<VolunteerLeaveVO>> list() {
        Long volunteerId = BaseContext.getLoginId();
        List<VolunteerLeaveVO> list = volunteerLeaveService.getListByVolunteerId(volunteerId);
        return Result.success(list);
    }

    @PostMapping("/submit")
    public Result<Void> submit(@RequestBody VolunteerLeave leave) {
        Long volunteerId = BaseContext.getLoginId();
        leave.setVolunteerId(volunteerId);
        volunteerLeaveService.submit(leave);
        return Result.success();
    }
}

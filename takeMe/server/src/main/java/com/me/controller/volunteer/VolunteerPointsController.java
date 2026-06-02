package com.me.controller.volunteer;

import com.me.context.BaseContext;
import com.me.result.Result;
import com.me.service.VolunteerPointsService;
import com.me.vo.VolunteerPointsRecordVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/volunteer/points")
@RequiredArgsConstructor
public class VolunteerPointsController {

    private final VolunteerPointsService volunteerPointsService;

    @GetMapping("/list")
    public Result<List<VolunteerPointsRecordVO>> list() {
        Long volunteerId = BaseContext.getLoginId();
        List<VolunteerPointsRecordVO> list = volunteerPointsService.getListByVolunteerId(volunteerId);
        return Result.success(list);
    }

    @GetMapping("/summary")
    public Result<VolunteerPointsRecordVO> summary() {
        Long volunteerId = BaseContext.getLoginId();
        VolunteerPointsRecordVO summary = volunteerPointsService.getSummary(volunteerId);
        return Result.success(summary);
    }
}

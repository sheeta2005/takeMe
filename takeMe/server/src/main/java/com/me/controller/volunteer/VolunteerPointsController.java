package com.me.controller.volunteer;

import com.me.context.BaseContext;
import com.me.result.Result;
import com.me.service.VolunteerPointsService;
import com.me.vo.VolunteerPointsRecordVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "志愿者-积分信息")
@RestController
@RequestMapping("/api/volunteer/points")
@RequiredArgsConstructor
public class VolunteerPointsController {

    private final VolunteerPointsService volunteerPointsService;

    @Operation(summary = "查积分列表")
    @GetMapping("/list")
    public Result<List<VolunteerPointsRecordVO>> list() {
        Long volunteerId = BaseContext.getLoginId();
        List<VolunteerPointsRecordVO> list = volunteerPointsService.getListByVolunteerId(volunteerId);
        return Result.success(list);
    }

    @Operation(summary = "查总积分")
    @GetMapping("/summary")
    public Result<VolunteerPointsRecordVO> summary() {
        Long volunteerId = BaseContext.getLoginId();
        VolunteerPointsRecordVO summary = volunteerPointsService.getSummary(volunteerId);
        return Result.success(summary);
    }

    @Operation(summary = "充值积分")
    @PostMapping("/add")
    public Result<String> addPoints(@RequestParam(defaultValue = "100") Integer points) {
        Long volunteerId = BaseContext.getLoginId();
        volunteerPointsService.addPoints(volunteerId, points);
        return Result.success("成功添加" + points + "积分");
    }
}

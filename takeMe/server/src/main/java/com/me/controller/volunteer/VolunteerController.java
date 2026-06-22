package com.me.controller.volunteer;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.me.context.BaseContext;
import com.me.entity.Approval;
import com.me.entity.OrderItem;
import com.me.entity.Volunteer;
import com.me.mapper.ApprovalMapper;
import com.me.mapper.OrderItemMapper;
import com.me.result.Result;
import com.me.service.VolunteerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Tag(name = "志愿者-信息管理")
@RestController
@RequestMapping("/api/volunteer")
@RequiredArgsConstructor
public class VolunteerController {

    private final VolunteerService volunteerService;
    private final OrderItemMapper orderItemMapper;
    private final ApprovalMapper approvalMapper;

    @Operation(summary = "查询信息")
    @GetMapping("/info")
    public Result<Volunteer> getInfo() {
        Long volunteerId = BaseContext.getLoginId();
        Volunteer volunteer = volunteerService.getById(volunteerId);

        if (volunteer == null) {
            return Result.error("志愿者不存在");
        }

        LambdaQueryWrapper<OrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderItem::getVolunteerId, volunteerId);
        wrapper.eq(OrderItem::getItemStatus, 3);
        Long completedServices = orderItemMapper.selectCount(wrapper);

        volunteer.setTotalServiceHours(completedServices.intValue());

        volunteer.setPassword(null);
        return Result.success(volunteer);
    }

    @Operation(summary = "更新信息")
    @PostMapping("/update")
    public Result<Void> update(@RequestBody Volunteer volunteer) {
        try {
            Long volunteerId = BaseContext.getLoginId();
            Volunteer existVolunteer = volunteerService.getById(volunteerId);
            if (existVolunteer == null) {
                return Result.error("志愿者不存在");
            }

            boolean serviceDaysChanged = volunteer.getServiceDays() != null
                    && !volunteer.getServiceDays().equals(existVolunteer.getServiceDays());

            volunteer.setId(volunteerId);
            volunteer.setUsername(null);
            volunteer.setPassword(null);
            volunteer.setStatus(null);
            volunteer.setCreateTime(null);
            volunteer.setLastLoginTime(null);
            volunteer.setWorkStatus(null);
            volunteer.setTotalServiceHours(null);

            if (serviceDaysChanged) {
                String newServiceDays = volunteer.getServiceDays();
                volunteer.setServiceDays(existVolunteer.getServiceDays());

                Approval approval = new Approval();
                approval.setType("service_days_change");
                approval.setApplicantId(volunteerId);
                approval.setApplicantName(existVolunteer.getRealName());
                approval.setContent(newServiceDays);
                approval.setStatus("pending");
                approval.setCreateTime(LocalDateTime.now());
                approvalMapper.insert(approval);

                volunteerService.updateById(volunteer);
                return Result.success();
            }

            volunteer.setServiceDays(null);
            volunteerService.updateById(volunteer);
            return Result.success();
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("修改失败：" + e.getMessage());
        }
    }

}
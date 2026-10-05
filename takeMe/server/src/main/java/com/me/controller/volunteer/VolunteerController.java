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
import org.springframework.transaction.annotation.Transactional;

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
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> update(@RequestBody Volunteer volunteer) {
        Long volunteerId = BaseContext.getLoginId();
        Volunteer existVolunteer = volunteerService.getById(volunteerId);
        if (existVolunteer == null) {
            return Result.error("志愿者不存在");
        }

        boolean serviceDaysChanged = volunteer.getServiceDays() != null
                && !volunteer.getServiceDays().equals(existVolunteer.getServiceDays());

        // 只复制可编辑资料，积分与账号字段不能从请求写回数据库。
        Volunteer profile = new Volunteer();
        profile.setId(volunteerId);
        profile.setRealName(volunteer.getRealName());
        profile.setPhone(volunteer.getPhone());
        profile.setAvatar(volunteer.getAvatar());
        profile.setGender(volunteer.getGender());
        profile.setAge(volunteer.getAge());
        profile.setAddress(volunteer.getAddress());
        profile.setEmergencyName(volunteer.getEmergencyName());
        profile.setEmergencyPhone(volunteer.getEmergencyPhone());

        if (serviceDaysChanged) {
            String newServiceDays = volunteer.getServiceDays();
            Approval approval = new Approval();
            approval.setType("service_days_change");
            approval.setApplicantId(volunteerId);
            approval.setBusinessId(volunteerId);
            approval.setApplicantName(existVolunteer.getRealName());
            approval.setContent(newServiceDays);
            approval.setStatus("pending");
            approval.setCreateTime(LocalDateTime.now());
            approvalMapper.insert(approval);
        }
        if (!volunteerService.updateById(profile)) {
            throw new IllegalStateException("资料修改失败");
        }
        return Result.success();
    }

}

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
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/volunteer")
@RequiredArgsConstructor
public class VolunteerController {

    private final VolunteerService volunteerService;
    private final OrderItemMapper orderItemMapper;
    private final ApprovalMapper approvalMapper;

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

    @PostMapping("/uploadAvatar")
    public Result<Map<String, String>> uploadAvatar(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam("file") MultipartFile file
    ) {
        if (file.isEmpty()) {
            return Result.error("上传文件不能为空");
        }

        try {
            String originalFilename = file.getOriginalFilename();
            String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
            String fileName = UUID.randomUUID() + suffix;

            String uploadPath = "D:/uploads/volunteer-avatar/";
            File dest = new File(uploadPath + fileName);
            if (!dest.getParentFile().exists()) {
                dest.getParentFile().mkdirs();
            }
            file.transferTo(dest);

            Long volunteerId = BaseContext.getLoginId();
            Volunteer volunteer = new Volunteer();
            volunteer.setId(volunteerId);
            String avatarUrl = "http://localhost:8080/uploads/volunteer-avatar/" + fileName;
            volunteer.setAvatar(avatarUrl);
            volunteerService.updateById(volunteer);

            Map<String, String> data = new HashMap<>();
            data.put("url", avatarUrl);
            return Result.success(data);
        } catch (IOException e) {
            e.printStackTrace();
            return Result.error("头像上传失败");
        }
    }
}
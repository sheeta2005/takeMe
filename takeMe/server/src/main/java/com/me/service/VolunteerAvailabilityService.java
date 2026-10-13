package com.me.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.me.entity.OrderItem;
import com.me.entity.Volunteer;
import com.me.entity.VolunteerLeave;
import com.me.exception.OrderBusinessException;
import com.me.mapper.OrderItemMapper;
import com.me.mapper.VolunteerLeaveMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Arrays;

// 资料展示和接单共用实时排班、请假判断，不依赖数据库中遗留的工作状态。
@Service
@RequiredArgsConstructor
public class VolunteerAvailabilityService {
    private final VolunteerLeaveMapper volunteerLeaveMapper;
    private final OrderItemMapper orderItemMapper;

    public int getWorkStatus(Volunteer volunteer) {
        return workStatusAt(volunteer, LocalDateTime.now(ZoneId.of("Asia/Shanghai")));
    }

    // 状态：0休息、1待命、2服务中、3请假中；周日沿用排班中的编号0。
    int workStatusAt(Volunteer volunteer, LocalDateTime now) {
        if (!Integer.valueOf(1).equals(volunteer.getStatus())) return 0;
        if (hasApprovedLeave(volunteer.getId(), now)) return 3;
        if (orderItemMapper.selectCount(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getVolunteerId, volunteer.getId()).eq(OrderItem::getItemStatus, 2)) > 0) return 2;
        String today = String.valueOf(now.getDayOfWeek().getValue() % 7);
        return volunteer.getServiceDays() != null && Arrays.stream(volunteer.getServiceDays().split(","))
                .map(String::trim).anyMatch(today::equals) ? 1 : 0;
    }

    // 只有批准的请假生效，起止时刻均计入请假范围。
    public boolean hasApprovedLeave(Long volunteerId, LocalDateTime time) {
        return volunteerLeaveMapper.selectCount(new LambdaQueryWrapper<VolunteerLeave>()
                .eq(VolunteerLeave::getVolunteerId, volunteerId).eq(VolunteerLeave::getStatus, 1)
                .le(VolunteerLeave::getStartTime, time).ge(VolunteerLeave::getEndTime, time)) > 0;
    }

    // 调用方持有志愿者行锁，与请假批准串行，防止审批和接单同时通过。
    public void validateCanAccept(Volunteer volunteer, OrderItem item) {
        int status = getWorkStatus(volunteer);
        if (status == 3) throw new OrderBusinessException("您当前处于请假状态，无法接取服务");
        if (status == 0) throw new OrderBusinessException("今天不是您的工作日，无法接取服务");
        LocalDateTime appointment = LocalDateTime.of(
                LocalDate.parse(item.getServiceDate()), LocalTime.parse(item.getServiceTime()));
        if (hasApprovedLeave(volunteer.getId(), appointment)) {
            throw new OrderBusinessException("服务预约时间与已批准请假冲突，无法接取服务");
        }
    }
}

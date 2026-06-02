package com.me.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.me.entity.VolunteerLeave;
import com.me.mapper.VolunteerLeaveMapper;
import com.me.service.VolunteerLeaveService;
import com.me.vo.VolunteerLeaveVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VolunteerLeaveServiceImpl implements VolunteerLeaveService {

    private final VolunteerLeaveMapper volunteerLeaveMapper;

    @Override
    public List<VolunteerLeaveVO> getListByVolunteerId(Long volunteerId) {
        LambdaQueryWrapper<VolunteerLeave> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VolunteerLeave::getVolunteerId, volunteerId);
        wrapper.orderByDesc(VolunteerLeave::getCreateTime);

        List<VolunteerLeave> leaves = volunteerLeaveMapper.selectList(wrapper);
        return leaves.stream().map(leave -> {
            VolunteerLeaveVO vo = new VolunteerLeaveVO();
            BeanUtils.copyProperties(leave, vo);
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public void submit(VolunteerLeave leave) {
        leave.setStatus(0);
        leave.setCreateTime(LocalDateTime.now());
        volunteerLeaveMapper.insert(leave);
    }
}

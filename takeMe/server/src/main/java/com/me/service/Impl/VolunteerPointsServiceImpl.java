package com.me.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.me.entity.Volunteer;
import com.me.entity.VolunteerPointsRecord;
import com.me.mapper.VolunteerMapper;
import com.me.mapper.VolunteerPointsRecordMapper;
import com.me.service.VolunteerPointsService;
import com.me.vo.VolunteerPointsRecordVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VolunteerPointsServiceImpl implements VolunteerPointsService {

    private final VolunteerPointsRecordMapper volunteerPointsRecordMapper;
    private final VolunteerMapper volunteerMapper;

    @Override
    public List<VolunteerPointsRecordVO> getListByVolunteerId(Long volunteerId) {
        LambdaQueryWrapper<VolunteerPointsRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VolunteerPointsRecord::getVolunteerId, volunteerId);
        wrapper.orderByDesc(VolunteerPointsRecord::getCreateTime);

        List<VolunteerPointsRecord> records = volunteerPointsRecordMapper.selectList(wrapper);
        return records.stream().map(record -> {
            VolunteerPointsRecordVO vo = new VolunteerPointsRecordVO();
            BeanUtils.copyProperties(record, vo);
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public VolunteerPointsRecordVO getSummary(Long volunteerId) {
        Volunteer volunteer = volunteerMapper.selectById(volunteerId);
        if (volunteer == null) {
            throw new RuntimeException("志愿者不存在");
        }

        LambdaQueryWrapper<VolunteerPointsRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VolunteerPointsRecord::getVolunteerId, volunteerId);

        List<VolunteerPointsRecord> records = volunteerPointsRecordMapper.selectList(wrapper);
        int totalPoints = records.stream().mapToInt(VolunteerPointsRecord::getPoints).sum();

        VolunteerPointsRecordVO summary = new VolunteerPointsRecordVO();
        summary.setVolunteerId(volunteerId);
        summary.setPoints(volunteer.getPoints() != null ? volunteer.getPoints() : 0);
        return summary;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addPoints(Long volunteerId, Integer points) {
        if (points == null || points <= 0) {
            throw new RuntimeException("积分数量必须大于0");
        }

        Volunteer volunteer = volunteerMapper.selectById(volunteerId);
        if (volunteer == null) {
            throw new RuntimeException("志愿者不存在");
        }

        int currentPoints = volunteer.getPoints() != null ? volunteer.getPoints() : 0;
        int newPoints = currentPoints + points;
        
        volunteer.setPoints(newPoints);
        volunteerMapper.updateById(volunteer);
        
        VolunteerPointsRecord record = new VolunteerPointsRecord();
        record.setVolunteerId(volunteerId);
        record.setPoints(points);
        record.setType(2);
        record.setDescription("手动充值获得" + points + "积分");
        record.setCreateTime(LocalDateTime.now());
        volunteerPointsRecordMapper.insert(record);
    }
}

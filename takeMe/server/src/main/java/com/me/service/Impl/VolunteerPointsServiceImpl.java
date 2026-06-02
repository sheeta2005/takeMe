package com.me.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.me.entity.VolunteerPointsRecord;
import com.me.mapper.VolunteerPointsRecordMapper;
import com.me.service.VolunteerPointsService;
import com.me.vo.VolunteerPointsRecordVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VolunteerPointsServiceImpl implements VolunteerPointsService {

    private final VolunteerPointsRecordMapper volunteerPointsRecordMapper;

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
        LambdaQueryWrapper<VolunteerPointsRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VolunteerPointsRecord::getVolunteerId, volunteerId);

        List<VolunteerPointsRecord> records = volunteerPointsRecordMapper.selectList(wrapper);
        int totalPoints = records.stream().mapToInt(VolunteerPointsRecord::getPoints).sum();

        VolunteerPointsRecordVO summary = new VolunteerPointsRecordVO();
        summary.setVolunteerId(volunteerId);
        summary.setPoints(totalPoints);
        return summary;
    }
}

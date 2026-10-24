package com.me.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.List;
import com.me.entity.VolunteerPointsRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface VolunteerPointsRecordMapper extends BaseMapper<VolunteerPointsRecord> {
    //统计已发放积分总额
    Long sumIssuedPoints();

    // 积分记录按原有创建时间排序。
    default List<VolunteerPointsRecord> selectHistory(Long volunteerId) {
        LambdaQueryWrapper<VolunteerPointsRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VolunteerPointsRecord::getVolunteerId, volunteerId);
        wrapper.orderByDesc(VolunteerPointsRecord::getCreateTime);

        return selectList(wrapper);
    }

    // 按志愿者读取积分流水，保留原查询范围。
    default List<VolunteerPointsRecord> selectByVolunteerId(Long volunteerId) {
        return selectList(new LambdaQueryWrapper<VolunteerPointsRecord>().eq(VolunteerPointsRecord::getVolunteerId, volunteerId));
    }
}

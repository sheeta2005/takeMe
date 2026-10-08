package com.me.mapper;

import com.me.entity.VolunteerPointsRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface VolunteerPointsRecordMapper extends BaseMapper<VolunteerPointsRecord> {
    //统计已发放积分总额
    @Select("SELECT COALESCE(SUM(points),0) FROM volunteer_points_record WHERE points > 0")
    Long sumIssuedPoints();
}

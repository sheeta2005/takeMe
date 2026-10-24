package com.me.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.List;
import java.time.LocalDateTime;
import com.me.entity.VolunteerLeave;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface VolunteerLeaveMapper extends BaseMapper<VolunteerLeave> {
    //查询并锁定请假记录
    VolunteerLeave selectForUpdate(@Param("id") Long id);

    // 请假审批保留原状态条件，避免重复状态迁移。
    int decidePending(@Param("id") Long id, @Param("status") Integer status);

    // 批准请假的起止时刻均包含在生效范围内。
    Long countApprovedAt(@Param("volunteerId") Long volunteerId, @Param("time") LocalDateTime time);

    // 请假历史按创建时间倒序查询。
    default List<VolunteerLeave> selectByVolunteerId(Long volunteerId) {
        LambdaQueryWrapper<VolunteerLeave> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VolunteerLeave::getVolunteerId, volunteerId);
        wrapper.orderByDesc(VolunteerLeave::getCreateTime);

        return selectList(wrapper);
    }
}

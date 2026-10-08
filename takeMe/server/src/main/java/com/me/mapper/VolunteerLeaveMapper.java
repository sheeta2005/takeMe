package com.me.mapper;

import com.me.entity.VolunteerLeave;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface VolunteerLeaveMapper extends BaseMapper<VolunteerLeave> {
    //查询并锁定请假记录
    @Select("SELECT * FROM volunteer_leave WHERE id = #{id} FOR UPDATE")
    @Options(useCache = false, flushCache = Options.FlushCachePolicy.TRUE)
    VolunteerLeave selectForUpdate(@Param("id") Long id);
}

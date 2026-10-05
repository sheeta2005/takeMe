package com.me.mapper;

import com.me.entity.Volunteer;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface VolunteerMapper extends BaseMapper<Volunteer> {

    // 同一志愿者的接单、放弃及完成按相同顺序先取得此锁。
    @Select("SELECT * FROM volunteer WHERE id = #{id} FOR UPDATE")
    @Options(useCache = false, flushCache = Options.FlushCachePolicy.TRUE)
    Volunteer selectForUpdate(@Param("id") Long id);
}

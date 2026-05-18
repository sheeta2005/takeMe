package com.me.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.me.entity.Volunteer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface VolunteerMapper extends BaseMapper<Volunteer> {

    /**
     * 查询所有志愿者（分页用）
     */
    List<Volunteer> listAll();

    // 条件分页查询
    List<Volunteer> findByCondition(@Param("username") String username,
                                    @Param("id") Integer id,
                                    @Param("availableRange") String availableRange);
}
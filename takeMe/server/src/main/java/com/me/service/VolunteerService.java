package com.me.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.github.pagehelper.PageInfo;
import com.me.dto.LoginDTO;
import com.me.dto.VolunteerMatchDTO;
import com.me.entity.Volunteer;
import com.me.vo.LoginVO;
import com.me.vo.VolunteerInfoVO;

import java.util.List;

public interface VolunteerService extends IService<Volunteer> {
    LoginVO login(LoginDTO loginDTO);

    VolunteerInfoVO getVolunteerInfo(Long volunteerId);

    List<VolunteerInfoVO> matchVolunteers(VolunteerMatchDTO dto);

    /**
     * 分页查询志愿者列表
     *
     * @param page     页码
     * @param pageSize 每页条数
     * @return 分页结果
     */
    PageInfo<Volunteer> pageList(Integer page, Integer pageSize);

    PageInfo<Volunteer> search(Integer page, Integer pageSize,
                               String username,
                               Integer id,
                               String availableRange);

    // 新增
    boolean add(Volunteer volunteer);

    // 修改
    boolean update(Volunteer volunteer);

    // 删除
    boolean delete(Integer id);


}
package com.me.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.me.dto.LoginDTO;
import com.me.dto.VolunteerMatchDTO;
import com.me.entity.Volunteer;
import com.me.mapper.VolunteerMapper;
import com.me.service.VolunteerService;
import com.me.vo.LoginVO;
import com.me.vo.VolunteerInfoVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import java.util.List;
import java.util.stream.Collectors;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.stereotype.Service;
@Service
public class VolunteerServiceImpl extends ServiceImpl<VolunteerMapper, Volunteer> implements VolunteerService {


    @Autowired
    VolunteerMapper volunteerMapper;


    @Override
    public PageInfo<Volunteer> search(Integer page, Integer pageSize, String username, Integer id, String availableRange) {
        PageHelper.startPage(page, pageSize);
        List<Volunteer> list = volunteerMapper.findByCondition(username, id, availableRange);
        return new PageInfo<>(list);
    }

    @Override
    public boolean add(Volunteer volunteer) {
        return volunteerMapper.insert(volunteer) > 0;
    }

    @Override
    public boolean update(Volunteer volunteer) {
        return volunteerMapper.updateById(volunteer) > 0;
    }

    @Override
    public boolean delete(Integer id) {
        return volunteerMapper.deleteById(id) > 0;
    }

    @Override
    public LoginVO login(LoginDTO loginDTO) {
        LambdaQueryWrapper<Volunteer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Volunteer::getAccount, loginDTO.getAccount());
        Volunteer volunteer = this.getOne(wrapper);
        if (volunteer == null) {
            throw new RuntimeException("账号不存在");
        }
        String encryptPwd = DigestUtils.md5DigestAsHex(loginDTO.getPassword().getBytes());
        if (!volunteer.getPassword().equals(encryptPwd)) {
            throw new RuntimeException("密码错误");
        }
        if (volunteer.getStatus() == 0) {
            throw new RuntimeException("账号已被禁用");
        }
        LoginVO loginVO = new LoginVO();
        loginVO.setToken("token_" + volunteer.getId());
        loginVO.setRole(2);
        loginVO.setUserId(volunteer.getId());
        return loginVO;
    }

    @Override
    public VolunteerInfoVO getVolunteerInfo(Long volunteerId) {
        Volunteer volunteer = this.getById(volunteerId);
        if (volunteer == null) {
            throw new RuntimeException("志愿者不存在");
        }
        VolunteerInfoVO vo = new VolunteerInfoVO();
        vo.setId(volunteer.getId());
        vo.setUsername(volunteer.getUsername());
        vo.setAccount(volunteer.getAccount());
        vo.setPhone(volunteer.getPhone());
        vo.setAge(volunteer.getAge());
        vo.setGender(volunteer.getGender());
        vo.setAddress(volunteer.getAddress());
        vo.setAvailableRange(volunteer.getAvailableRange());
        vo.setFreeTime(volunteer.getFreeTime());
        vo.setWorkDay(volunteer.getWorkDay());
        vo.setCurrentStatus(volunteer.getCurrentStatus());
        vo.setAvatar(volunteer.getAvatar());
        vo.setStatus(volunteer.getStatus());
        return vo;
    }

    @Override
    public List<VolunteerInfoVO> matchVolunteers(VolunteerMatchDTO dto) {
        LambdaQueryWrapper<Volunteer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Volunteer::getAvailableRange, dto.getServiceArea())
                .eq(Volunteer::getWorkDay, dto.getServiceDay())
                .eq(Volunteer::getFreeTime, dto.getServiceTime())
                .eq(Volunteer::getCurrentStatus, 1)
                .eq(Volunteer::getStatus, 1);
        List<Volunteer> list = this.list(wrapper);
        return list.stream().map(v -> {
            VolunteerInfoVO vo = new VolunteerInfoVO();
            vo.setId(v.getId());
            vo.setUsername(v.getUsername());
            vo.setAccount(v.getAccount());
            vo.setPhone(v.getPhone());
            vo.setAge(v.getAge());
            vo.setGender(v.getGender());
            vo.setAddress(v.getAddress());
            vo.setAvailableRange(v.getAvailableRange());
            vo.setFreeTime(v.getFreeTime());
            vo.setWorkDay(v.getWorkDay());
            vo.setCurrentStatus(v.getCurrentStatus());
            vo.setAvatar(v.getAvatar());
            vo.setStatus(v.getStatus());
            return vo;
        }).collect(Collectors.toList());
    }


    @Override
    public PageInfo<Volunteer> pageList(Integer page, Integer pageSize) {
        // 开启分页（必须在查询前执行）
        PageHelper.startPage(page, pageSize);
        // 执行查询
        List<Volunteer> list = volunteerMapper.listAll();
        // 封装分页结果
        return new PageInfo<>(list);
    }








}
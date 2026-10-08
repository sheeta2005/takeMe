package com.me.service;

import com.me.vo.VolunteerPointsRecordVO;

import java.util.List;

public interface VolunteerPointsService {

    //查询志愿者积分记录
    List<VolunteerPointsRecordVO> getListByVolunteerId(Long volunteerId);

    //查询志愿者积分余额
    VolunteerPointsRecordVO getSummary(Long volunteerId);

    //充值志愿者积分并登记流水
    void addPoints(Long volunteerId, Integer points);
}


package com.me.service;

import com.me.entity.VolunteerLeave;
import com.me.vo.VolunteerLeaveVO;

import java.util.List;

public interface VolunteerLeaveService {

    //查询志愿者请假记录
    List<VolunteerLeaveVO> getListByVolunteerId(Long volunteerId);

    //提交请假申请
    void submit(VolunteerLeave leave);
}

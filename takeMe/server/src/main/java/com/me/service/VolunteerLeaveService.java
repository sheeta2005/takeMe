
package com.me.service;

import com.me.entity.VolunteerLeave;
import com.me.vo.VolunteerLeaveVO;

import java.util.List;

public interface VolunteerLeaveService {

    List<VolunteerLeaveVO> getListByVolunteerId(Long volunteerId);

    void submit(VolunteerLeave leave);
}

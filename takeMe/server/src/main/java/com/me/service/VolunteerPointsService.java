package com.me.service;

import com.me.vo.VolunteerPointsRecordVO;

import java.util.List;

public interface VolunteerPointsService {

    List<VolunteerPointsRecordVO> getListByVolunteerId(Long volunteerId);

    VolunteerPointsRecordVO getSummary(Long volunteerId);
}

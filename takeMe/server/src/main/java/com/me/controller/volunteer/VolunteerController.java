package com.me.controller.volunteer;

import com.me.dto.LoginDTO;
import com.me.dto.VolunteerMatchDTO;
import com.me.service.VolunteerService;
import com.me.vo.LoginVO;
import com.me.vo.ResultVO;
import com.me.vo.VolunteerInfoVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/volunteer")
public class VolunteerController {

    @Autowired
    private VolunteerService volunteerService;

    @PostMapping("/login")
    public ResultVO<LoginVO> login(@RequestBody LoginDTO loginDTO) {
        try {
            return ResultVO.success(volunteerService.login(loginDTO));
        } catch (Exception e) {
            return ResultVO.error(e.getMessage());
        }
    }

    @GetMapping("/info/{volunteerId}")
    public ResultVO<VolunteerInfoVO> getVolunteerInfo(@PathVariable Long volunteerId) {
        try {
            return ResultVO.success(volunteerService.getVolunteerInfo(volunteerId));
        } catch (Exception e) {
            return ResultVO.error(e.getMessage());
        }
    }

    @PostMapping("/match")
    public ResultVO<List<VolunteerInfoVO>> matchVolunteers(@RequestBody VolunteerMatchDTO dto) {
        try {
            return ResultVO.success(volunteerService.matchVolunteers(dto));
        } catch (Exception e) {
            return ResultVO.error(e.getMessage());
        }
    }
}
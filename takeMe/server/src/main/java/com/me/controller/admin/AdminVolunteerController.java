package com.me.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.me.entity.Volunteer;
import com.me.service.VolunteerService;
import com.me.result.Result;
import com.me.vo.PageResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/volunteer")
@RequiredArgsConstructor
public class AdminVolunteerController {

    private final VolunteerService volunteerService;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/page")
    public Result<PageResultVO<Volunteer>> getVolunteerPage(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        Page<Volunteer> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<Volunteer> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Volunteer::getCreateTime);
        
        Page<Volunteer> mpPage = volunteerService.page(pageParam, wrapper);
        mpPage.getRecords().forEach(v -> v.setPassword(null));
        
        PageResultVO<Volunteer> result = new PageResultVO<>(mpPage.getTotal(), mpPage.getRecords());
        return Result.success(result);
    }

    @GetMapping("/search")
    public Result<PageResultVO<Volunteer>> searchVolunteer(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Long id
    ) {
        Page<Volunteer> mpPage = volunteerService.searchVolunteer(page, pageSize, username, id);
        mpPage.getRecords().forEach(v -> v.setPassword(null));
        
        PageResultVO<Volunteer> result = new PageResultVO<>(mpPage.getTotal(), mpPage.getRecords());
        return Result.success(result);
    }

    @GetMapping("/detail/{id}")
    public Result<Volunteer> getVolunteerDetail(@PathVariable Long id) {
        Volunteer volunteer = volunteerService.getById(id);
        if (volunteer == null) {
            return Result.error("志愿者不存在");
        }
        volunteer.setPassword(null);
        return Result.success(volunteer);
    }

    @PostMapping("/add")
    public Result<Void> addVolunteer(@RequestBody Volunteer volunteer) {
        LambdaQueryWrapper<Volunteer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Volunteer::getUsername, volunteer.getUsername());
        Long count = volunteerService.count(wrapper);
        if (count > 0) {
            return Result.error("账号已存在");
        }
        
        volunteer.setPassword(passwordEncoder.encode("123456"));
        volunteer.setStatus(1);
        volunteerService.save(volunteer);
        
        return Result.success();
    }

    @PostMapping("/update")
    public Result<Void> updateVolunteer(@RequestBody Volunteer volunteer) {
        Volunteer existVolunteer = volunteerService.getById(volunteer.getId());
        if (existVolunteer == null) {
            return Result.error("志愿者不存在");
        }
        
        volunteer.setUsername(null);
        volunteer.setPassword(null);
        volunteer.setCreateTime(null);
        volunteer.setLastLoginTime(null);
        
        volunteerService.updateById(volunteer);
        return Result.success();
    }

    @DeleteMapping("/delete/{id}")
    public Result<Void> deleteVolunteer(@PathVariable Long id) {
        Volunteer volunteer = volunteerService.getById(id);
        if (volunteer == null) {
            return Result.error("志愿者不存在");
        }
        
        volunteerService.removeById(id);
        return Result.success();
    }

    @PostMapping("/status/{id}")
    public Result<Void> updateVolunteerStatus(
            @PathVariable Long id,
            @RequestParam Integer status
    ) {
        Volunteer volunteer = volunteerService.getById(id);
        if (volunteer == null) {
            return Result.error("志愿者不存在");
        }
        
        volunteer.setStatus(status);
        volunteerService.updateById(volunteer);
        return Result.success();
    }
}

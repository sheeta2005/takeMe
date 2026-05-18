package com.me.controller.admin;

import com.github.pagehelper.PageInfo;
import com.me.dto.LoginDTO;
import com.me.entity.Volunteer;
import com.me.service.AdminService;
import com.me.service.VolunteerService;
import com.me.vo.LoginVO;
import com.me.vo.ResultVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    @Autowired
    private AdminService adminService;

    @Autowired
    private VolunteerService volunteerService;

    /**
     * 分页查询志愿者列表
     *
     * @param page     页码，默认1
     * @param pageSize 每页条数，默认10
     * @return 分页结果
     */
    @GetMapping("/volunteer/page")
    public ResultVO<PageInfo<Volunteer>> volunteerPage(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        PageInfo<Volunteer> pageInfo = volunteerService.pageList(page, pageSize);
        return ResultVO.success(pageInfo);
    }

    // 搜索 + 分页
    @GetMapping("/volunteer/search")
    public ResultVO search(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Integer id,
            @RequestParam(required = false) String availableRange) {
        PageInfo<Volunteer> info = volunteerService.search(page, pageSize, username, id, availableRange);
        return ResultVO.success(info);
    }

    // 新增
    @PostMapping("/volunteer/add")
    public ResultVO add(@RequestBody Volunteer volunteer) {
        boolean b = volunteerService.add(volunteer);
        return b ? ResultVO.success("添加成功") : ResultVO.error("添加失败");
    }

    // 修改（启用/停用）
    @PostMapping("/volunteer/update")
    public ResultVO update(@RequestBody Volunteer volunteer) {
        boolean b = volunteerService.update(volunteer);
        return b ? ResultVO.success("修改成功") : ResultVO.error("修改失败");
    }

    // 删除
    @GetMapping("/volunteer/delete")
    public ResultVO delete(@RequestParam Integer id) {
        boolean b = volunteerService.delete(id);
        return b ? ResultVO.success("删除成功") : ResultVO.error("删除失败");
    }


    @PostMapping("/login")
    public ResultVO<LoginVO> login(@RequestBody LoginDTO loginDTO) {
        try {
            return ResultVO.success(adminService.login(loginDTO));
        } catch (Exception e) {
            return ResultVO.error(e.getMessage());
        }
    }
}
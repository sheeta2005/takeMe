package com.me.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.me.dto.PageResultDTO;
import com.me.entity.User;
import com.me.result.Result;
import com.me.service.UserService;
import com.me.vo.PageResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/user")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;

    @GetMapping("/page")
    public Result<PageResultVO<User>> getUserPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        PageResultDTO pageResultDTO = new PageResultDTO();
        pageResultDTO.setPageNum(pageNum);
        pageResultDTO.setPageSize(pageSize);
        
        IPage<User> iPage = userService.searchUser(null, null, null, null, null, pageResultDTO);
        iPage.getRecords().forEach(u -> u.setPassword(null));
        
        PageResultVO<User> result = PageResultVO.from(iPage);
        return Result.success(result);
    }

    @GetMapping("/search")
    public Result<PageResultVO<User>> searchUser(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer gender,
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        PageResultDTO pageResultDTO = new PageResultDTO();
        pageResultDTO.setPageNum(pageNum);
        pageResultDTO.setPageSize(pageSize);
        
        IPage<User> iPage = userService.searchUser(keyword, gender, id, startDate, endDate, pageResultDTO);
        iPage.getRecords().forEach(u -> u.setPassword(null));
        
        PageResultVO<User> result = PageResultVO.from(iPage);
        return Result.success(result);
    }

    @GetMapping("/detail/{id}")
    public Result<User> getUserDetail(@PathVariable Long id) {
        User user = userService.getById(id);
        if (user == null) {
            return Result.error("用户不存在");
        }
        user.setPassword(null);
        return Result.success(user);
    }

    @DeleteMapping("/delete/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        User user = userService.getById(id);
        if (user == null) {
            return Result.error("用户不存在");
        }
        
        boolean success = userService.logicalDeleteUser(id);
        
        if (!success) {
            return Result.error("删除失败");
        }
        return Result.success();
    }

    @PostMapping("/status/{id}")

    public Result<Void> updateUserStatus(@PathVariable Long id, @RequestParam Integer status) {
        User user = userService.getById(id);
        if (user == null) {
            return Result.error("用户不存在");
        }
        
        user.setStatus(status);
        boolean success = userService.updateById(user);
        
        if (!success) {
            return Result.error("状态更新失败");
        }
        return Result.success();
    }
}

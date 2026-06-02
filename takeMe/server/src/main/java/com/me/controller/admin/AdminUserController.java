package com.me.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.me.entity.User;
import com.me.service.UserService;
import com.me.result.Result;
import com.me.vo.PageResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/user")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/page")
    public Result<PageResultVO<User>> getUserPage(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        Page<User> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(User::getCreateTime);
        
        Page<User> mpPage = userService.page(pageParam, wrapper);
        mpPage.getRecords().forEach(u -> u.setPassword(null));
        
        PageResultVO<User> result = new PageResultVO<>(mpPage.getTotal(), mpPage.getRecords());
        return Result.success(result);
    }

    @GetMapping("/search")
    public Result<PageResultVO<User>> searchUser(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer gender,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        Page<User> mpPage = userService.searchUser(page, pageSize, keyword, gender, startDate, endDate);
        mpPage.getRecords().forEach(u -> u.setPassword(null));
        
        PageResultVO<User> result = new PageResultVO<>(mpPage.getTotal(), mpPage.getRecords());
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

    @PostMapping("/add")
    public Result<Void> addUser(@RequestBody User user) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, user.getUsername());
        Long count = userService.count(wrapper);
        if (count > 0) {
            return Result.error("账号已存在");
        }
        
        user.setPassword(passwordEncoder.encode("123456"));
        user.setStatus(1);
        userService.save(user);
        
        return Result.success();
    }

    @PostMapping("/update")
    public Result<Void> updateUser(@RequestBody User user) {
        User existUser = userService.getById(user.getId());
        if (existUser == null) {
            return Result.error("用户不存在");
        }
        
        user.setUsername(null);
        user.setPassword(null);
        user.setStatus(null);
        user.setCreateTime(null);
        user.setLastLoginTime(null);
        
        userService.updateById(user);
        return Result.success();
    }

    @DeleteMapping("/delete/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        User user = userService.getById(id);
        if (user == null) {
            return Result.error("用户不存在");
        }
        
        userService.removeById(id);
        return Result.success();
    }

    @PostMapping("/status/{id}")
    public Result<Void> updateUserStatus(
            @PathVariable Long id,
            @RequestParam Integer status
    ) {
        User user = userService.getById(id);
        if (user == null) {
            return Result.error("用户不存在");
        }
        
        user.setStatus(status);
        userService.updateById(user);
        return Result.success();
    }
}

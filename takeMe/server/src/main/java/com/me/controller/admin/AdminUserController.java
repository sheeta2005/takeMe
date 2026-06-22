package com.me.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.me.dto.PageResultDTO;
import com.me.entity.Address;
import com.me.entity.User;
import com.me.mapper.AddressMapper;
import com.me.result.Result;
import com.me.service.UserService;
import com.me.vo.PageResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "管理员-用户管理")
@RestController
@RequestMapping("/api/admin/user")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;
    private final AddressMapper addressMapper;

    @Operation(summary = "分页查")
    @GetMapping("/page")
    public Result<PageResultVO<Map<String, Object>>> getUserPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        PageResultDTO pageResultDTO = new PageResultDTO();
        pageResultDTO.setPageNum(pageNum);
        pageResultDTO.setPageSize(pageSize);
        
        IPage<User> iPage = userService.searchUser(null, null, null, null, null, pageResultDTO, null, null);
        iPage.getRecords().forEach(u -> u.setPassword(null));
        
        IPage<Map<String, Object>> resultPage = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(iPage.getCurrent(), iPage.getSize(), iPage.getTotal());
        java.util.List<Map<String, Object>> enrichedRecords = iPage.getRecords().stream().map(user -> {
            Map<String, Object> record = new HashMap<>();
            record.put("id", user.getId());
            record.put("realName", user.getRealName());
            record.put("username", user.getUsername());
            record.put("phone", user.getPhone());
            record.put("avatar", user.getAvatar());
            record.put("gender", user.getGender());
            record.put("age", user.getAge());
            record.put("status", user.getStatus());
            record.put("createTime", user.getCreateTime());
            
            LambdaQueryWrapper<Address> addrWrapper = new LambdaQueryWrapper<>();
            addrWrapper.eq(Address::getUserId, user.getId())
                       .eq(Address::getIsDefault, 1)
                       .last("LIMIT 1");
            Address defaultAddress = addressMapper.selectOne(addrWrapper);
            record.put("address", defaultAddress != null ? defaultAddress.getAddress() : "");
            
            return record;
        }).collect(java.util.stream.Collectors.toList());
        
        resultPage.setRecords(enrichedRecords);
        PageResultVO<Map<String, Object>> result = PageResultVO.from(resultPage);
        return Result.success(result);
    }
    @Operation(summary = "搜索")
    @GetMapping("/search")
    public Result<PageResultVO<Map<String, Object>>> searchUser(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer gender,
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortOrder
    ) {
        PageResultDTO pageResultDTO = new PageResultDTO();
        pageResultDTO.setPageNum(pageNum);
        pageResultDTO.setPageSize(pageSize);
        
        IPage<User> iPage = userService.searchUser(keyword, gender, id, startDate, endDate, pageResultDTO, sortBy, sortOrder);
        iPage.getRecords().forEach(u -> u.setPassword(null));
        
        IPage<Map<String, Object>> resultPage = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(iPage.getCurrent(), iPage.getSize(), iPage.getTotal());
        java.util.List<Map<String, Object>> enrichedRecords = iPage.getRecords().stream().map(user -> {
            Map<String, Object> record = new HashMap<>();
            record.put("id", user.getId());
            record.put("realName", user.getRealName());
            record.put("username", user.getUsername());
            record.put("phone", user.getPhone());
            record.put("avatar", user.getAvatar());
            record.put("gender", user.getGender());
            record.put("age", user.getAge());
            record.put("status", user.getStatus());
            record.put("createTime", user.getCreateTime());
            record.put("lastLoginTime", user.getLastLoginTime());
            
            LambdaQueryWrapper<Address> addrWrapper = new LambdaQueryWrapper<>();
            addrWrapper.eq(Address::getUserId, user.getId())
                       .eq(Address::getIsDefault, 1)
                       .last("LIMIT 1");
            Address defaultAddress = addressMapper.selectOne(addrWrapper);
            record.put("address", defaultAddress != null ? defaultAddress.getAddress() : "");
            
            return record;
        }).collect(java.util.stream.Collectors.toList());
        
        resultPage.setRecords(enrichedRecords);
        PageResultVO<Map<String, Object>> result = PageResultVO.from(resultPage);
        return Result.success(result);
    }

    @Operation(summary = "查详情")
    @GetMapping("/detail/{id}")
    public Result<Map<String, Object>> getUserDetail(@PathVariable Long id) {
        User user = userService.getById(id);
        if (user == null) {
            return Result.error("用户不存在");
        }
        user.setPassword(null);
        
        Map<String, Object> result = new HashMap<>();
        result.put("id", user.getId());
        result.put("realName", user.getRealName());
        result.put("username", user.getUsername());
        result.put("phone", user.getPhone());
        result.put("avatar", user.getAvatar());
        result.put("gender", user.getGender());
        result.put("age", user.getAge());
        result.put("status", user.getStatus());
        result.put("createTime", user.getCreateTime());
        result.put("lastLoginTime", user.getLastLoginTime());
        result.put("emergencyName", user.getEmergencyName());
        result.put("emergencyPhone", user.getEmergencyPhone());
        
        LambdaQueryWrapper<Address> addrWrapper = new LambdaQueryWrapper<>();
        addrWrapper.eq(Address::getUserId, user.getId())
                   .eq(Address::getIsDefault, 1)
                   .last("LIMIT 1");
        Address defaultAddress = addressMapper.selectOne(addrWrapper);
        result.put("address", defaultAddress != null ? defaultAddress.getAddress() : "");
        
        return Result.success(result);
    }

    @Operation(summary = "逻辑删")
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
    @Operation(summary = "改状态")
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

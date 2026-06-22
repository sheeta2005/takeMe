package com.me.controller.user;

import com.me.context.BaseContext;
import com.me.result.Result;
import com.me.service.AddressService;
import com.me.vo.AddressVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "用户-地址管理")
@RestController
@RequestMapping("/api/user/address")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    /**
     * 获取当前用户所有地址
     */
    @Operation(summary = "查所有地址")
    @GetMapping("/list")
    public Result<List<AddressVO>> list() {
        Long userId = BaseContext.getLoginId();
        List<AddressVO> list = addressService.getListByUserId(userId);
        return Result.success(list);
    }

    /**
     * 新增地址
     */
    @Operation(summary = "增")
    @PostMapping("/add")
    public Result<AddressVO> add(@RequestBody AddressVO vo) {
        Long userId = BaseContext.getLoginId();
        vo.setUserId(userId);
        return Result.success(addressService.add(vo));
    }

    /**
     * 修改地址
     */
    @Operation(summary = "改")
    @PostMapping("/update")
    public Result<Void> update(@RequestBody AddressVO vo) {
        addressService.update(vo);
        return Result.success();
    }

    /**
     * 删除地址
     */
    @Operation(summary = "删")
    @PostMapping("/delete")
    public Result<Void> delete(@RequestParam Long id) {
        addressService.delete(id);
        return Result.success();
    }

    /**
     * 设置默认地址
     */
    @Operation(summary = "改默认")
    @PostMapping("/default")
    public Result<Void> setDefault(@RequestParam Long id) {
        addressService.setDefault(id);
        return Result.success();
    }
}
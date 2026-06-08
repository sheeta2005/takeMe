package com.me.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.me.entity.ServicePackage;
import com.me.result.Result;
import com.me.service.ServicePackageService;
import com.me.vo.PageResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/service-package")
@RequiredArgsConstructor
public class AdminServicePackageController {

    private final ServicePackageService servicePackageService;

    @GetMapping("/page")
    public Result<PageResultVO<ServicePackage>> getServicePackagePage(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword
    ) {
        Page<ServicePackage> mpPage = servicePackageService.searchServicePackage(page, pageSize, type, status, keyword);
        PageResultVO<ServicePackage> result = new PageResultVO<>(mpPage.getTotal(), mpPage.getRecords());
        return Result.success(result);
    }

    @GetMapping("/detail/{id}")
    public Result<ServicePackage> getServicePackageDetail(@PathVariable Long id) {
        ServicePackage servicePackage = servicePackageService.getById(id);
        if (servicePackage == null) {
            return Result.error("服务套餐不存在");
        }
        return Result.success(servicePackage);
    }

    @PostMapping("/add")
    public Result<Void> addServicePackage(@RequestBody ServicePackage servicePackage) {
        servicePackageService.save(servicePackage);
        return Result.success();
    }

    @PostMapping("/update")
    public Result<Void> updateServicePackage(@RequestBody ServicePackage servicePackage) {
        ServicePackage existPackage = servicePackageService.getById(servicePackage.getId());
        if (existPackage == null) {
            return Result.error("服务套餐不存在");
        }
        servicePackageService.updateById(servicePackage);
        return Result.success();
    }

    @DeleteMapping("/delete/{id}")
    public Result<Void> deleteServicePackage(@PathVariable Long id) {
        ServicePackage servicePackage = servicePackageService.getById(id);
        if (servicePackage == null) {
            return Result.error("服务套餐不存在");
        }
        servicePackageService.removeById(id);
        return Result.success();
    }

    @PostMapping("/status/{id}")
    public Result<Void> updateServicePackageStatus(
            @PathVariable Long id,
            @RequestParam Integer status
    ) {
        ServicePackage servicePackage = servicePackageService.getById(id);
        if (servicePackage == null) {
            return Result.error("服务套餐不存在");
        }
        servicePackage.setStatus(status);
        servicePackageService.updateById(servicePackage);
        return Result.success();
    }
}

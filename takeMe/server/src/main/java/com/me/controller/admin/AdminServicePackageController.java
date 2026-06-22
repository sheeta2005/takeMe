package com.me.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.me.dto.PageResultDTO;
import com.me.entity.ServicePackage;
import com.me.result.Result;
import com.me.service.ServicePackageService;
import com.me.vo.PageResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "管理员-服务项目种类管理")
@RestController
@RequestMapping("/api/admin/service-package")
@RequiredArgsConstructor
public class AdminServicePackageController {

    private final ServicePackageService servicePackageService;

    @Operation(summary = "分页查")
    @GetMapping("/page")
    public Result<PageResultVO<ServicePackage>> getServicePackagePage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword
    ) {
        PageResultDTO pageResultDTO = new PageResultDTO();
        pageResultDTO.setPageNum(pageNum);
        pageResultDTO.setPageSize(pageSize);

        IPage<ServicePackage> iPage = servicePackageService.searchServicePackage(type, status, keyword, pageResultDTO);
        PageResultVO<ServicePackage> result = PageResultVO.from(iPage);
        return Result.success(result);
    }

    @Operation(summary = "增")
    @PostMapping("/create")
    public Result<Void> createServicePackage(@RequestBody ServicePackage servicePackage) {
        boolean success = servicePackageService.save(servicePackage);
        if (!success) {
            return Result.error("创建失败");
        }
        return Result.success();
    }

    @Operation(summary = "改")
    @PostMapping("/update")
    public Result<Void> updateServicePackage(@RequestBody ServicePackage servicePackage) {
        boolean success = servicePackageService.updateById(servicePackage);
        if (!success) {
            return Result.error("更新失败");
        }
        return Result.success();
    }

    @Operation(summary = "删")
    @PostMapping("/delete/{id}")
    public Result<Void> deleteServicePackage(@PathVariable Long id) {
        boolean success = servicePackageService.removeById(id);
        if (!success) {
            return Result.error("删除失败");
        }
        return Result.success();
    }
}

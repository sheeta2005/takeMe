package com.me.controller.user;

import com.me.entity.ServicePackage;
import com.me.result.Result;
import com.me.service.ServicePackageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/user/service")
@RequiredArgsConstructor
public class ServiceController {

    private final ServicePackageService servicePackageService;

    /**
     * 根据服务类型获取可用服务列表
     * @param type 服务类型：0=代购 1=助洁 2=助餐 3=助医 4=陪伴
     */
    @GetMapping("/list")
    public Result<List<ServicePackage>> getServiceList(@RequestParam Integer type) {
        List<ServicePackage> list = servicePackageService.getAvailableServiceByType(type);
        return Result.success(list);
    }
}
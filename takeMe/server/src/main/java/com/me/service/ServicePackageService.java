package com.me.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.me.entity.ServicePackage;

import java.util.List;

public interface ServicePackageService extends IService<ServicePackage> {

    Page<ServicePackage> searchServicePackage(
            Integer page,
            Integer pageSize,
            Integer type,
            Integer status,
            String keyword
    );

    /**
     * 根据服务类型获取可用服务列表
     * @param type 服务类型：0=代购 1=助洁 2=助餐 3=助医 4=陪伴
     * @return 启用状态的服务列表
     */
    List<ServicePackage> getAvailableServiceByType(Integer type);
}
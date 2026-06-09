package com.me.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.me.dto.PageResultDTO;
import com.me.entity.ServicePackage;

public interface ServicePackageService extends IService<ServicePackage> {
    IPage<ServicePackage> searchServicePackage(
            Integer type,
            Integer status,
            String keyword,
            PageResultDTO pageResultDTO
    );
}
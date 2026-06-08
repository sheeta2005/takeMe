package com.me.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.me.entity.ServicePackage;
import com.me.mapper.ServicePackageMapper;
import com.me.service.ServicePackageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ServicePackageServiceImpl
        extends ServiceImpl<ServicePackageMapper, ServicePackage>
        implements ServicePackageService {

    @Override
    public Page<ServicePackage> searchServicePackage(
            Integer page,
            Integer pageSize,
            Integer type,
            Integer status,
            String keyword
    ) {
        Page<ServicePackage> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<ServicePackage> wrapper = new LambdaQueryWrapper<>();

        if (type != null) {
            wrapper.eq(ServicePackage::getType, type);
        }
        if (status != null) {
            wrapper.eq(ServicePackage::getStatus, status);
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.like(ServicePackage::getName, keyword);
        }

        wrapper.orderByDesc(ServicePackage::getCreateTime);
        return this.page(pageParam, wrapper);
    }

    @Override
    public List<ServicePackage> getAvailableServiceByType(Integer type) {
        LambdaQueryWrapper<ServicePackage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ServicePackage::getType, type)
                .eq(ServicePackage::getStatus, 1); // 只返回启用的服务
        return this.list(wrapper);
    }
}
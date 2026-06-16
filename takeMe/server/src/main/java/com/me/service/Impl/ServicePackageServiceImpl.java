package com.me.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.me.dto.PageResultDTO;
import com.me.entity.ServicePackage;
import com.me.mapper.ServicePackageMapper;
import com.me.redis.annotation.RedisCache;
import com.me.service.ServicePackageService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ServicePackageServiceImpl extends ServiceImpl<ServicePackageMapper, ServicePackage> implements ServicePackageService {

    @Override
    public IPage<ServicePackage> searchServicePackage(
            Integer type,
            Integer status,
            String keyword,
            PageResultDTO pageResultDTO
    ) {
        Page<ServicePackage> page = new Page<>(pageResultDTO.getPageNum(), pageResultDTO.getPageSize());

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
        return this.page(page, wrapper);
    }

    @Override
    @RedisCache(prefix = "service:available", expire = 120, nullExpire = 2)
    public List<ServicePackage> getAvailableServiceByType(Integer type) {
        LambdaQueryWrapper<ServicePackage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ServicePackage::getType, type);
        wrapper.eq(ServicePackage::getStatus, 1);
        wrapper.orderByAsc(ServicePackage::getCreateTime);
        return this.list(wrapper);
    }
}
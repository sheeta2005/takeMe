package com.me.mapper;

import java.util.List;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.me.entity.ServicePackage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ServicePackageMapper extends BaseMapper<ServicePackage> {

    // 服务套餐使用单表条件分页。
    default Page<ServicePackage> selectSearchPage(Page<ServicePackage> page, Integer type, Integer status, String keyword) {
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
        return selectPage(page, wrapper);
    }

    // 按类型查询上架套餐并保持原创建时间排序。
    default List<ServicePackage> selectAvailableByType(Integer type) {
        LambdaQueryWrapper<ServicePackage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(type != null, ServicePackage::getType, type);
        wrapper.eq(ServicePackage::getStatus, 1);
        wrapper.orderByAsc(ServicePackage::getCreateTime);
        return selectList(wrapper);
    }
}
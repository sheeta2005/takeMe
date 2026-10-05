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
import com.me.redis.utils.RedisUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.io.Serializable;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.serializer.SerializationException;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ServicePackageServiceImpl extends ServiceImpl<ServicePackageMapper, ServicePackage> implements ServicePackageService {
    private final RedisUtil redisUtil;

    @Override
    public boolean save(ServicePackage service) {
        boolean changed = super.save(service);
        if (changed) evict(service.getType());
        return changed;
    }

    @Override
    public boolean updateById(ServicePackage service) {
        ServicePackage old = getById(service.getId());
        boolean changed = super.updateById(service);
        if (changed) {
            if (old != null) evict(old.getType());
            evict(service.getType());
        }
        return changed;
    }

    @Override
    public boolean removeById(Serializable id) {
        ServicePackage old = getById(id);
        boolean changed = super.removeById(id);
        if (changed && old != null) evict(old.getType());
        return changed;
    }

    private void evict(Integer type) {
        Runnable eviction = () -> {
            try {
                redisUtil.delete("service:available:" + type);
                redisUtil.delete("null:service:available:" + type);
                redisUtil.delete("service:available:null");
                redisUtil.delete("null:service:available:null");
            } catch (DataAccessException | SerializationException ex) {
                // 数据库提交不可回滚为缓存失败；短 TTL 兜底，目录不是支付或权限真值。
                log.warn("目录缓存失效失败，等待短TTL更新，原因={}", ex.getClass().getSimpleName());
            }
        };
        // 外层存在事务时，必须等数据库提交后再失效目录缓存。
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { eviction.run(); }
            });
        } else {
            eviction.run();
        }
    }

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
    @RedisCache(prefix = "service:available", keyArgs = {0}, expire = 2, nullExpire = 2)
    public List<ServicePackage> getAvailableServiceByType(Integer type) {
        LambdaQueryWrapper<ServicePackage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(type != null, ServicePackage::getType, type);
        wrapper.eq(ServicePackage::getStatus, 1);
        wrapper.orderByAsc(ServicePackage::getCreateTime);
        return this.list(wrapper);
    }
}

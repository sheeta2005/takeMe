package com.me.service.Impl;

import com.me.context.BaseContext;
import com.me.entity.Address;
import com.me.mapper.AddressMapper;
import com.me.service.AddressService;
import com.me.vo.AddressVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final AddressMapper addressMapper;

    @Override
    public List<AddressVO> getListByUserId(Long userId) {
        LambdaQueryWrapper<Address> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Address::getUserId, userId).orderByDesc(Address::getIsDefault);
        List<Address> list = addressMapper.selectList(wrapper);
        return list.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public AddressVO add(AddressVO vo) {
        // 如果设置为默认，先清空其他默认
        if (vo.getIsDefault() == 1) {
            clearDefault(vo.getUserId());
        }
        Address address = new Address();
        BeanUtils.copyProperties(vo, address);
        address.setCreateTime(LocalDateTime.now());
        addressMapper.insert(address);
        vo.setId(address.getId());
        return vo;
    }

    @Override
    public void update(AddressVO vo) {
        Address address = addressMapper.selectById(vo.getId());
        if (vo.getIsDefault() == 1) {
            clearDefault(address.getUserId());
        }
        BeanUtils.copyProperties(vo, address);
        addressMapper.updateById(address);
    }

    @Override
    public void delete(Long id) {
        addressMapper.deleteById(id);
    }

    @Override
    public void setDefault(Long id) {
        Address address = addressMapper.selectById(id);
        clearDefault(address.getUserId());
        address.setIsDefault(1);
        addressMapper.updateById(address);
    }

    // 清空默认地址
    private void clearDefault(Long userId) {
        LambdaUpdateWrapper<Address> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Address::getUserId, userId).set(Address::getIsDefault, 0);
        addressMapper.update(null, wrapper);
    }

    private AddressVO toVO(Address address) {
        AddressVO vo = new AddressVO();
        BeanUtils.copyProperties(address, vo);
        return vo;
    }
}
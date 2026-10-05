package com.me.service.Impl;

import com.me.context.BaseContext;
import com.me.entity.Address;
import com.me.mapper.AddressMapper;
import com.me.mapper.UserMapper;
import com.me.service.AddressService;
import com.me.vo.AddressVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final AddressMapper addressMapper;
    private final UserMapper userMapper;

    @Override
    public List<AddressVO> getListByUserId(Long userId) {
        LambdaQueryWrapper<Address> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Address::getUserId, userId).orderByDesc(Address::getIsDefault);
        List<Address> list = addressMapper.selectList(wrapper);
        return list.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AddressVO add(AddressVO vo) {
        Long userId = lockCurrentUser();
        int defaultValue = defaultValue(vo.getIsDefault());
        // 如果设置为默认，先清空其他默认
        if (defaultValue == 1) {
            clearDefault(userId);
        }
        Address address = new Address();
        // 仅接收可编辑字段，归属和主键由服务端确定。
        address.setUserId(userId);
        address.setAddress(vo.getAddress());
        address.setIsDefault(defaultValue);
        address.setCreateTime(LocalDateTime.now());
        addressMapper.insert(address);
        return toVO(address);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(AddressVO vo) {
        Long userId = lockCurrentUser();
        Address address = ownedAddress(vo.getId(), userId);
        int defaultValue = vo.getIsDefault() == null ? address.getIsDefault() : defaultValue(vo.getIsDefault());
        if (defaultValue == 1) {
            clearDefault(address.getUserId());
        }
        address.setAddress(vo.getAddress());
        address.setIsDefault(defaultValue);
        addressMapper.updateById(address);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ownedAddress(id, lockCurrentUser());
        addressMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefault(Long id) {
        Address address = ownedAddress(id, lockCurrentUser());
        clearDefault(address.getUserId());
        address.setIsDefault(1);
        addressMapper.updateById(address);
    }

    private Long lockCurrentUser() {
        Long userId = BaseContext.getLoginId();
        if (userId == null || userMapper.selectForUpdate(userId) == null) {
            throw new AccessDeniedException("账号不存在");
        }
        return userId;
    }

    private Address ownedAddress(Long id, Long userId) {
        Address address = addressMapper.selectOne(new LambdaQueryWrapper<Address>()
                .eq(Address::getId, id).eq(Address::getUserId, userId));
        if (address == null) throw new AccessDeniedException("无权操作此地址");
        return address;
    }

    private int defaultValue(Integer value) {
        if (value == null) return 0;
        if (value != 0 && value != 1) throw new IllegalArgumentException("默认地址标识无效");
        return value;
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

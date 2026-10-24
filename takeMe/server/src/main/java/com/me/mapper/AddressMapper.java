package com.me.mapper;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.List;
import com.me.entity.Address;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AddressMapper extends BaseMapper<Address> {

    // 查询用户地址，默认地址优先。
    default List<Address> selectByUserId(Long userId) {
        LambdaQueryWrapper<Address> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Address::getUserId, userId).orderByDesc(Address::getIsDefault);
        return selectList(wrapper);
    }

    // 按主键和所属用户共同查询地址。
    default Address selectOwned(Long id, Long userId) {
        return selectOne(new LambdaQueryWrapper<Address>().eq(Address::getId, id).eq(Address::getUserId, userId));
    }

    // 修改默认地址前清除同账号的默认标识。
    default int clearDefaults(Long userId) {
        LambdaUpdateWrapper<Address> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Address::getUserId, userId).set(Address::getIsDefault, 0);
        return update(null, wrapper);
    }

    // 沿用默认地址查询的单条限制。
    default Address selectDefaultByUserId(Long userId) {
        return selectOne(new LambdaQueryWrapper<Address>().eq(Address::getUserId, userId)
                .eq(Address::getIsDefault, 1).last("LIMIT 1"));
    }
}
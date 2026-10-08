package com.me.service;

import com.me.vo.AddressVO;
import java.util.List;

public interface AddressService {
    //查询用户地址列表
    List<AddressVO> getListByUserId(Long userId);
    //新增用户地址
    AddressVO add(AddressVO vo);
    //修改用户地址
    void update(AddressVO vo);
    //删除用户地址
    void delete(Long id);
    //设置默认地址
    void setDefault(Long id);
}
package com.me.service;

import com.me.vo.AddressVO;
import java.util.List;

public interface AddressService {
    List<AddressVO> getListByUserId(Long userId);
    AddressVO add(AddressVO vo);
    void update(AddressVO vo);
    void delete(Long id);
    void setDefault(Long id);
}
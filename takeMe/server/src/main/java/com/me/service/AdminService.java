package com.me.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.me.dto.LoginDTO;
import com.me.entity.Admin;
import com.me.vo.LoginVO;

public interface AdminService extends IService<Admin> {
    LoginVO login(LoginDTO loginDTO);


    Admin getById(Long id);
}
}
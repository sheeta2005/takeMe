package com.me.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.me.entity.Admin;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AdminMapper extends BaseMapper<Admin> {


    // 按登录账号查询资料。
    default Admin selectByUsername(String username) {
        LambdaQueryWrapper<Admin> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Admin::getUsername, username);
        return selectOne(queryWrapper);
    }

    // 头像允许显式清空，避免非空更新策略保留旧值。
    int updateAvatar(@Param("id") Long id, @Param("avatar") String avatar);

    // 读取需要接收审批通知的管理员。
    default List<Admin> selectAllAdmins() {
        return selectList(new LambdaQueryWrapper<Admin>());
    }
}
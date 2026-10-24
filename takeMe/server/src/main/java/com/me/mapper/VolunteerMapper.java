package com.me.mapper;

import java.util.List;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.me.entity.Volunteer;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface VolunteerMapper extends BaseMapper<Volunteer> {

    // 同一志愿者的接单、放弃及完成按相同顺序先取得此锁。
    Volunteer selectForUpdate(@Param("id") Long id);

    // 按登录账号查询资料。
    default Volunteer selectByUsername(String username) {
        LambdaQueryWrapper<Volunteer> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Volunteer::getUsername, username);
        return selectOne(queryWrapper);
    }

    // 头像允许显式清空，避免非空更新策略保留旧值。
    int updateAvatar(@Param("id") Long id, @Param("avatar") String avatar);

    // 编号分页保持固定投影，避免读取账号完整资料。
    Page<Volunteer> selectEnabledIdPage(@Param("page") Page<Volunteer> page);

    // 群发按主键续页，避免账号停用导致偏移分页漏人。
    List<Long> selectEnabledIdsAfter(@Param("lastId") long lastId);

    // 统计启用账号数量。
    Long countEnabled();

    // 验证账号仍处于启用状态。
    Long countActiveAccount(@Param("id") Long id);

    // 志愿者单表筛选与排序由 Mapper 构造。
    default Page<Volunteer> selectSearchPage(Page<Volunteer> pageParam, String username, Long id, String keyword, String sortBy, String sortOrder) {
        LambdaQueryWrapper<Volunteer> wrapper = new LambdaQueryWrapper<>();

//        if (username != null && !username.trim().isEmpty()) {
//            wrapper.like(Volunteer::getUsername, username);
//        }
        if (id != null) {
            wrapper.eq(Volunteer::getId, id);
        }

        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.and(w -> w.like(Volunteer::getUsername, keyword)
                    .or()
                    .like(Volunteer::getRealName, keyword)
                    .or()
                    .like(Volunteer::getPhone, keyword));
        } else if (username != null && !username.trim().isEmpty()) {
            wrapper.like(Volunteer::getUsername, username);
        }

        if ("lastLoginTime".equals(sortBy)) {
            if ("asc".equalsIgnoreCase(sortOrder)) {
                wrapper.orderByAsc(Volunteer::getLastLoginTime);
            } else {
                wrapper.orderByDesc(Volunteer::getLastLoginTime);
            }
        } else {
            wrapper.orderByDesc(Volunteer::getCreateTime);
        }

        return selectPage(pageParam, wrapper);
    }

    // 清空志愿者敏感资料，不覆盖积分及服务累计值。
    int anonymize(@Param("id") Long id, @Param("realName") String realName, @Param("username") String username, @Param("phone") String phone);
}

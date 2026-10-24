package com.me.mapper;

import java.util.List;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.me.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    // 按老人账号串行化建单和默认地址变更，事务提交即释放。
    User selectForUpdate(@Param("id") Long id);

    // 按登录账号查询资料。
    default User selectByUsername(String username) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, username);
        return selectOne(queryWrapper);
    }

    // 头像允许显式清空，避免非空更新策略保留旧值。
    int updateAvatar(@Param("id") Long id, @Param("avatar") String avatar);

    // 用户单表筛选和排序统一由 Mapper 构造。
    default Page<User> selectSearchPage(Page<User> pageParam, String keyword, Integer gender, Long id, String startDate, String endDate, String sortBy, String sortOrder) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();

        if (id != null) {
            wrapper.eq(User::getId, id);
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.and(w -> w.like(User::getUsername, keyword)
                    .or()
                    .like(User::getRealName, keyword)
                    .or()
                    .like(User::getPhone, keyword));
        }
        if (gender != null) {
            wrapper.eq(User::getGender, gender);
        }
        if (startDate != null && !startDate.trim().isEmpty()) {
            wrapper.ge(User::getCreateTime, startDate + " 00:00:00");
        }
        if (endDate != null && !endDate.trim().isEmpty()) {
            wrapper.le(User::getCreateTime, endDate + " 23:59:59");
        }

        if ("lastLoginTime".equals(sortBy)) {
            if ("asc".equalsIgnoreCase(sortOrder)) {
                wrapper.orderByAsc(User::getLastLoginTime);
            } else {
                wrapper.orderByDesc(User::getLastLoginTime);
            }
        } else {
            wrapper.orderByDesc(User::getCreateTime);
        }

        return selectPage(pageParam, wrapper);
    }

    // 只写入新密码，保留账号其他并发字段。
    default int updatePassword(Long userId, String password) {
        return update(null, new LambdaUpdateWrapper<User>().eq(User::getId, userId).set(User::getPassword, password));
    }

    // 编号分页保持固定投影，避免读取账号完整资料。
    Page<User> selectEnabledIdPage(@Param("page") Page<User> page);

    // 群发按主键续页，避免账号停用导致偏移分页漏人。
    List<Long> selectEnabledIdsAfter(@Param("lastId") long lastId);

    // 统计启用账号数量。
    Long countEnabled();

    // 验证账号仍处于启用状态。
    Long countActiveAccount(@Param("id") Long id);

    // 注册前统计同名账号。
    Long countByUsername(@Param("username") String username);

    // 账号脱敏保留原字段更新范围，并显式清空头像。
    int anonymize(User user);
}

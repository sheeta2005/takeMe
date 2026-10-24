package com.me.mapper;

import java.util.List;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.me.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDateTime;
import java.util.Map;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {
    // 冷看板一次扫描汇总五个订单指标，避免对同一订单表重复 COUNT。
    Map<String, Object> selectDashboardOrderCounts(@Param("start") LocalDateTime start,
                                                  @Param("end") LocalDateTime end);

    //按提交标识查询并锁定订单
    Order selectByRequestForUpdate(@Param("userId") Long userId, @Param("requestId") String requestId);

    //查询并锁定订单
    Order selectForUpdate(@Param("id") Long id);

    // 用不可变的所属订单定位后锁住父订单，不预读服务项的业务状态。
    Order selectByItemForUpdate(@Param("itemId") Long itemId);

    // 查询用户订单，保留创建时间及主键的稳定排序。
    default Page<Order> selectUserPage(Page<Order> page, Long userId, Integer status, String orderNo) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getUserId, userId);
        if (status != null) {
            wrapper.eq(Order::getStatus, status);
        }
        if (orderNo != null && !orderNo.trim().isEmpty()) {
            wrapper.like(Order::getOrderNo, orderNo);
        }
        wrapper.orderByDesc(Order::getCreateTime, Order::getId);

        return selectPage(page, wrapper);
    }

    // 管理端普通订单列表使用单表状态分页。
    default Page<Order> selectStatusPage(Page<Order> pageParam, Integer status) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();

        if (status != null) {
            wrapper.eq(Order::getStatus, status);
        }

        wrapper.orderByDesc(Order::getCreateTime);
        return selectPage(pageParam, wrapper);
    }

    // 取消订单保留原状态条件并显式清空志愿者。
    int cancelFromStatus(@Param("id") Long id, @Param("oldStatus") Integer oldStatus);

    // 汇总志愿者为空时必须明确写入 NULL。
    int updateVolunteerIds(@Param("id") Long id, @Param("volunteerIds") String volunteerIds);

    // 管理端含数据库表达式的组合筛选统一由 XML 承担。
    Page<Order> selectAdminSearchPage(@Param("page") Page<Order> page, @Param("status") Integer status, @Param("orderNo") String orderNo, @Param("userId") Long userId, @Param("volunteerId") Long volunteerId, @Param("startDate") String startDate, @Param("endDate") String endDate);

    // 统计全部订单或指定状态数量。
    Long countByStatus(@Param("status") Integer status);

    // 按左闭右开时间范围统计订单。
    Long countCreatedBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // 保留用户首页原订单状态统计口径。
    Long countUncancelledForUser(@Param("userId") Long userId);

    // 保留用户首页原订单状态统计口径。
    Long countCompletedForUser(@Param("userId") Long userId);

    // 保留用户首页原订单状态统计口径。
    Long countPendingForUser(@Param("userId") Long userId);

    // 筛选完成三天后可补默认评价的订单。
    default List<Order> selectReviewCandidates(LocalDateTime threeDaysAgo) {
        LambdaQueryWrapper<Order> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.eq(Order::getStatus, 4);
        orderWrapper.le(Order::getCompleteTime, threeDaysAgo);
        return selectList(orderWrapper);
    }

    // 筛选超时待确认订单，实际迁移前由调用方重新加锁校验。
    default List<Order> selectAutoCompleteCandidates(LocalDateTime yesterday) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getStatus, 3);
        wrapper.and(w -> w.le(Order::getCompleteTime, yesterday)
                .or()
                .isNull(Order::getCompleteTime)
                .le(Order::getCreateTime, yesterday));

        return selectList(wrapper);
    }

    // 扫描超时未支付订单，逐项交由业务事务复核。
    default List<Order> selectUnpaidBefore(LocalDateTime cutoff) {
        return selectList(new LambdaQueryWrapper<Order>().eq(Order::getStatus, 6).le(Order::getCreateTime, cutoff));
    }
}

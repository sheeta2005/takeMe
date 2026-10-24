package com.me.mapper;

import java.util.Collection;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.me.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {

    // 状态检查及整单汇总都使用当前读，且清理 MyBatis 会话缓存。
    OrderItem selectForUpdate(@Param("id") Long id);

    //查询并锁定订单服务项
    List<OrderItem> selectByOrderForUpdate(@Param("orderId") Long orderId);

    // 调用方已锁志愿者账号；只检查活动项，不在父订单前持有服务项锁。
    List<OrderItem> selectActiveForVolunteer(@Param("volunteerId") Long volunteerId);

    // 显式写入 NULL，不能依赖实体默认的非空更新策略。
    int clearVolunteer(@Param("id") Long id, @Param("oldStatus") Integer oldStatus,
                       @Param("status") Integer status, @Param("volunteerId") Long volunteerId);

    //按原状态更新服务项
    int changeStatus(@Param("id") Long id, @Param("oldStatus") Integer oldStatus, @Param("status") Integer status);

    // 查询志愿者服务列表并保持原分页排序。
    default Page<OrderItem> selectVolunteerPage(Page<OrderItem> page, Long volunteerId, Integer status) {
        LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(OrderItem::getVolunteerId, volunteerId);
        if (status != null) {
            itemWrapper.eq(OrderItem::getItemStatus, status);
        }
        itemWrapper.orderByDesc(OrderItem::getCreateTime, OrderItem::getId);

        return selectPage(page, itemWrapper);
    }

    // 可接服务跨表筛选保留日期索引范围和跨午夜边界。
    Page<OrderItem> selectAvailablePage(@Param("page") Page<OrderItem> page, @Param("volunteerId") Long volunteerId, @Param("startDate") String startDate, @Param("endDate") String endDate, @Param("startTime") String startTime, @Param("endTime") String endTime);

    // 按本页订单批量读取服务项，保持主键顺序。
    default List<OrderItem> selectByOrderIds(Collection<Long> orderIds) {
        return selectList(new LambdaQueryWrapper<OrderItem>().in(OrderItem::getOrderId, orderIds).orderByAsc(OrderItem::getId));
    }

    // 读取订单内全部服务项。
    default List<OrderItem> selectByOrderId(Long orderId) {
        return selectList(new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId));
    }

    // 只读取当前志愿者在该订单中的服务项。
    default List<OrderItem> selectForOrderVolunteer(Long orderId, Long volunteerId) {
        return selectList(new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId).eq(OrderItem::getVolunteerId, volunteerId));
    }

    // 读取可参与默认评价的已完成服务项。
    default List<OrderItem> selectCompletedByOrderId(Long orderId) {
        return selectList(new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId).in(OrderItem::getItemStatus, 3, 4));
    }

    // 读取停用志愿者时需要释放的活动服务项。
    default List<OrderItem> selectAssignedToVolunteer(Long volunteerId) {
        return selectList(new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getVolunteerId, volunteerId).in(OrderItem::getItemStatus, 1, 2));
    }

    // 定时任务按服务状态读取候选项。
    default List<OrderItem> selectByStatus(Integer status) {
        return selectList(new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getItemStatus, status));
    }

    // 接单 CAS 必须同时匹配待接状态和空志愿者。
    int claimPending(@Param("id") Long id, @Param("volunteerId") Long volunteerId);

    // 验证志愿者是否参与该订单。
    Long countForOrderVolunteer(@Param("orderId") Long orderId, @Param("volunteerId") Long volunteerId);

    // 按志愿者和原服务状态统计数量。
    Long countForVolunteerStatus(@Param("volunteerId") Long volunteerId, @Param("status") Integer status);

    // 统计服务类型分布。
    Long countByServiceType(@Param("type") Integer type);
}

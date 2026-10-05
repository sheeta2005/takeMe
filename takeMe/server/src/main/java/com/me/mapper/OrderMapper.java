package com.me.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.me.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Options;
import java.time.LocalDateTime;
import java.util.Map;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {
    // 冷看板一次扫描汇总五个订单指标，避免对同一订单表重复 COUNT。
    @Select("""
            SELECT COUNT(*) AS totalOrders,
              COALESCE(SUM(status IN (1,2)),0) AS activeOrders,
              COALESCE(SUM(status = 0),0) AS pendingOrders,
              COALESCE(SUM(status = 4),0) AS completedOrders,
              COALESCE(SUM(create_time >= #{start} AND create_time < #{end}),0) AS todayOrders
            FROM `order`
            """)
    Map<String, Object> selectDashboardOrderCounts(@Param("start") LocalDateTime start,
                                                  @Param("end") LocalDateTime end);

    @Select("SELECT * FROM `order` WHERE user_id = #{userId} AND request_id = #{requestId} FOR UPDATE")
    @Options(useCache = false, flushCache = Options.FlushCachePolicy.TRUE)
    Order selectByRequestForUpdate(@Param("userId") Long userId, @Param("requestId") String requestId);

    @Select("SELECT * FROM `order` WHERE id = #{id} FOR UPDATE")
    @Options(useCache = false, flushCache = Options.FlushCachePolicy.TRUE)
    Order selectForUpdate(@Param("id") Long id);

    // 用不可变的所属订单定位后锁住父订单，不预读服务项的业务状态。
    @Select("SELECT * FROM `order` WHERE id = (SELECT order_id FROM order_item WHERE id = #{itemId}) FOR UPDATE")
    @Options(useCache = false, flushCache = Options.FlushCachePolicy.TRUE)
    Order selectByItemForUpdate(@Param("itemId") Long itemId);
}

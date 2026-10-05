package com.me.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.me.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.util.List;

@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {

    // 状态检查及整单汇总都使用当前读，且清理 MyBatis 会话缓存。
    @Select("SELECT * FROM order_item WHERE id = #{id} FOR UPDATE")
    @Options(useCache = false, flushCache = Options.FlushCachePolicy.TRUE)
    OrderItem selectForUpdate(@Param("id") Long id);

    @Select("SELECT * FROM order_item WHERE order_id = #{orderId} ORDER BY id FOR UPDATE")
    @Options(useCache = false, flushCache = Options.FlushCachePolicy.TRUE)
    List<OrderItem> selectByOrderForUpdate(@Param("orderId") Long orderId);

    // 调用方已锁志愿者账号；只检查活动项，不在父订单前持有服务项锁。
    @Select("SELECT * FROM order_item WHERE volunteer_id = #{volunteerId} AND item_status IN (1, 2) ORDER BY id")
    @Options(useCache = false, flushCache = Options.FlushCachePolicy.TRUE)
    List<OrderItem> selectActiveForVolunteer(@Param("volunteerId") Long volunteerId);

    // 显式写入 NULL，不能依赖实体默认的非空更新策略。
    @Update("UPDATE order_item SET volunteer_id = NULL, item_status = #{status} WHERE id = #{id} AND item_status = #{oldStatus} AND volunteer_id <=> #{volunteerId}")
    int clearVolunteer(@Param("id") Long id, @Param("oldStatus") Integer oldStatus,
                       @Param("status") Integer status, @Param("volunteerId") Long volunteerId);

    @Update("UPDATE order_item SET item_status = #{status} WHERE id = #{id} AND item_status = #{oldStatus}")
    int changeStatus(@Param("id") Long id, @Param("oldStatus") Integer oldStatus, @Param("status") Integer status);
}

package com.me.mapper;

import java.util.Collection;
import java.util.List;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.me.entity.Review;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ReviewMapper extends BaseMapper<Review> {
    // 调用方先锁主订单及服务项；当前读避免并发等待后读取旧评价快照。
    Review selectByItemForUpdate(@Param("itemId") Long itemId);

    // 评价列表按评分筛选并分页。
    default Page<Review> selectRatingPage(Page<Review> page, Integer rating) {
        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<>();

        if (rating != null) {
            wrapper.eq(Review::getRating, rating);
        }

        wrapper.orderByDesc(Review::getCreateTime);
        return selectPage(page, wrapper);
    }

    // 统计全部评价或指定评分数量。
    Long countByRating(@Param("rating") Integer rating);

    // 按本页订单批量读取评价。
    default List<Review> selectByOrderIds(Collection<Long> orderIds) {
        return selectList(new LambdaQueryWrapper<Review>().in(Review::getOrderId, orderIds));
    }

    // 读取订单的评价记录。
    default List<Review> selectByOrderId(Long orderId) {
        return selectList(new LambdaQueryWrapper<Review>().eq(Review::getOrderId, orderId));
    }

    // 读取用户评价，保留原有返回顺序。
    default List<Review> selectByUserId(Long userId) {
        return selectList(new LambdaQueryWrapper<Review>().eq(Review::getUserId, userId));
    }

    // 读取志愿者评价，保留原有返回顺序。
    default List<Review> selectByVolunteerId(Long volunteerId) {
        return selectList(new LambdaQueryWrapper<Review>().eq(Review::getVolunteerId, volunteerId));
    }

    // 管理端按时间倒序读取志愿者评价。
    default List<Review> selectRecentByVolunteerId(Long volunteerId) {
        return selectList(new LambdaQueryWrapper<Review>().eq(Review::getVolunteerId, volunteerId).orderByDesc(Review::getCreateTime));
    }

    // 默认评价前统计该服务项已有评价。
    Long countByItemId(@Param("itemId") Long itemId);
}

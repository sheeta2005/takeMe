package com.me.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.me.entity.Review;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ReviewMapper extends BaseMapper<Review> {
    // 调用方先锁主订单及服务项；当前读避免并发等待后读取旧评价快照。
    @Select("SELECT * FROM review WHERE order_item_id = #{itemId} FOR UPDATE")
    @Options(useCache = false, flushCache = Options.FlushCachePolicy.TRUE)
    Review selectByItemForUpdate(@Param("itemId") Long itemId);
}

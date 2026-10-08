package com.me.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.me.dto.PageResultDTO;
import com.me.entity.Review;

import java.util.Map;

public interface ReviewService extends IService<Review> {
    //分页查询评价
    IPage<Review> getReviewPage(Integer rating, PageResultDTO pageResultDTO);

    //查询评价详情
    Review getReviewDetail(Long id);

    //删除评价
    boolean deleteReview(Long id);

    //统计评价数量与评分分布
    Map<String, Object> getReviewStatistics();
}

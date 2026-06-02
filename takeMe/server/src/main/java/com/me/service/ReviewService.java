package com.me.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.me.entity.Review;

import java.util.Map;

public interface ReviewService extends IService<Review> {

    /**
     * 分页查询评价列表
     */
    Page<Review> getReviewPage(Integer page, Integer pageSize, Integer rating);

    /**
     * 获取评价详情
     */
    Review getReviewDetail(Long id);

    /**
     * 删除评价
     */
    boolean deleteReview(Long id);

    /**
     * 获取评价统计
     */
    Map<String, Object> getReviewStatistics();
}

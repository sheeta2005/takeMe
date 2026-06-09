package com.me.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.me.dto.PageResultDTO;
import com.me.entity.Review;

import java.util.Map;

public interface ReviewService extends IService<Review> {
    IPage<Review> getReviewPage(Integer rating, PageResultDTO pageResultDTO);

    Review getReviewDetail(Long id);

    boolean deleteReview(Long id);

    Map<String, Object> getReviewStatistics();
}

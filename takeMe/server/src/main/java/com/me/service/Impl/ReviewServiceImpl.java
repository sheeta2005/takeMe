package com.me.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.me.entity.Review;
import com.me.mapper.ReviewMapper;
import com.me.service.ReviewService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class ReviewServiceImpl extends ServiceImpl<ReviewMapper, Review> implements ReviewService {

    @Override
    public Page<Review> getReviewPage(Integer page, Integer pageSize, Integer rating) {
        Page<Review> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<>();

        if (rating != null) {
            wrapper.eq(Review::getRating, rating);
        }

        wrapper.orderByDesc(Review::getCreateTime);
        return this.page(pageParam, wrapper);
    }

    @Override
    public Review getReviewDetail(Long id) {
        return this.getById(id);
    }

    @Override
    public boolean deleteReview(Long id) {
        return this.removeById(id);
    }

    @Override
    public Map<String, Object> getReviewStatistics() {
        Map<String, Object> stats = new HashMap<>();

        LambdaQueryWrapper<Review> allWrapper = new LambdaQueryWrapper<>();
        Long totalCount = this.count(allWrapper);
        stats.put("totalCount", totalCount);

        for (int i = 1; i <= 5; i++) {
            LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Review::getRating, i);
            Long count = this.count(wrapper);
            stats.put("rating" + i + "Count", count);
        }

        return stats;
    }
}

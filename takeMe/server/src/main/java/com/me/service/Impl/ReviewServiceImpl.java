package com.me.service.Impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.me.dto.PageResultDTO;
import com.me.entity.Review;
import com.me.mapper.ReviewMapper;
import com.me.service.ReviewService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class ReviewServiceImpl extends ServiceImpl<ReviewMapper, Review> implements ReviewService {

    //分页查询评价
    @Override
    public IPage<Review> getReviewPage(Integer rating, PageResultDTO pageResultDTO) {
        Page<Review> page = new Page<>(pageResultDTO.getPageNum(), pageResultDTO.getPageSize());

        return baseMapper.selectRatingPage(page, rating);
    }

    //查询评价详情
    @Override
    public Review getReviewDetail(Long id) {
        return this.getById(id);
    }

    //删除评价
    @Override
    public boolean deleteReview(Long id) {
        return this.removeById(id);
    }

    //统计评价数量与评分分布
    @Override
    public Map<String, Object> getReviewStatistics() {
        Map<String, Object> stats = new HashMap<>();

        Long totalCount = baseMapper.countByRating(null);
        stats.put("totalCount", totalCount);

        for (int i = 1; i <= 5; i++) {
            Long count = baseMapper.countByRating(i);
            stats.put("rating" + i + "Count", count);
        }

        return stats;
    }
}

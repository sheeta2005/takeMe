package com.me.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.me.entity.Review;
import com.me.result.Result;
import com.me.service.ReviewService;
import com.me.vo.PageResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/review")
@RequiredArgsConstructor
public class AdminReviewController {

    private final ReviewService reviewService;

    @GetMapping("/page")
    public Result<PageResultVO<Review>> getReviewPage(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer rating
    ) {
        Page<Review> mpPage = reviewService.getReviewPage(page, pageSize, rating);
        PageResultVO<Review> result = new PageResultVO<>(mpPage.getTotal(), mpPage.getRecords());
        return Result.success(result);
    }

    @GetMapping("/detail/{id}")
    public Result<Review> getReviewDetail(@PathVariable Long id) {
        Review review = reviewService.getReviewDetail(id);
        if (review == null) {
            return Result.error("评价不存在");
        }
        return Result.success(review);
    }

    @DeleteMapping("/delete/{id}")
    public Result<Void> deleteReview(@PathVariable Long id) {
        boolean success = reviewService.deleteReview(id);
        if (!success) {
            return Result.error("评价不存在");
        }
        return Result.success();
    }

    @GetMapping("/statistics")
    public Result<Map<String, Object>> getReviewStatistics() {
        Map<String, Object> stats = reviewService.getReviewStatistics();
        return Result.success(stats);
    }
}

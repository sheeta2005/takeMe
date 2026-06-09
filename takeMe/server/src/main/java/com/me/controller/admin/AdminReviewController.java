package com.me.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.me.dto.PageResultDTO;
import com.me.entity.Review;
import com.me.result.Result;
import com.me.service.ReviewService;
import com.me.vo.PageResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/review")
@RequiredArgsConstructor
public class AdminReviewController {

    private final ReviewService reviewService;

    @GetMapping("/page")
    public Result<PageResultVO<Review>> getReviewPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer rating
    ) {
        PageResultDTO pageResultDTO = new PageResultDTO();
        pageResultDTO.setPageNum(pageNum);
        pageResultDTO.setPageSize(pageSize);
        
        IPage<Review> iPage = reviewService.getReviewPage(rating, pageResultDTO);
        PageResultVO<Review> result = PageResultVO.from(iPage);
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

    @PostMapping("/delete/{id}")
    public Result<Void> deleteReview(@PathVariable Long id) {
        boolean success = reviewService.deleteReview(id);
        if (!success) {
            return Result.error("删除失败");
        }
        return Result.success();
    }

    @GetMapping("/statistics")
    public Result<Object> getReviewStatistics() {
        return Result.success(reviewService.getReviewStatistics());
    }
}

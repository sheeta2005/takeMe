package com.me.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.me.dto.PageResultDTO;
import com.me.entity.OrderItem;
import com.me.entity.Review;
import com.me.entity.Volunteer;
import com.me.mapper.OrderItemMapper;
import com.me.mapper.ReviewMapper;
import com.me.result.Result;
import com.me.service.VolunteerService;
import com.me.vo.PageResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/admin/volunteer")
@RequiredArgsConstructor
@Slf4j
public class AdminVolunteerController {

    private final VolunteerService volunteerService;
    private final PasswordEncoder passwordEncoder;
    private final OrderItemMapper orderItemMapper;
    private final ReviewMapper reviewMapper;

    @GetMapping("/page")
    public Result<PageResultVO<Volunteer>> getVolunteerPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        PageResultDTO pageResultDTO = new PageResultDTO();
        pageResultDTO.setPageNum(pageNum);
        pageResultDTO.setPageSize(pageSize);
        
        IPage<Volunteer> iPage = volunteerService.searchVolunteer(null, null, pageResultDTO, null, null, null);
        iPage.getRecords().forEach(v -> v.setPassword(null));
        
        PageResultVO<Volunteer> result = PageResultVO.from(iPage);
        return Result.success(result);
    }

    @GetMapping("/search")
    public Result<PageResultVO<Volunteer>> searchVolunteer(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortOrder
    ) {
        PageResultDTO pageResultDTO = new PageResultDTO();
        pageResultDTO.setPageNum(pageNum);
        pageResultDTO.setPageSize(pageSize);
        
        IPage<Volunteer> iPage = volunteerService.searchVolunteer(username, id, pageResultDTO, keyword, sortBy, sortOrder);
        iPage.getRecords().forEach(v -> v.setPassword(null));
        
        PageResultVO<Volunteer> result = PageResultVO.from(iPage);
        return Result.success(result);
    }

    @GetMapping("/detail/{id}")
    public Result<Map<String, Object>> getVolunteerDetail(@PathVariable Long id) {
        Volunteer volunteer = volunteerService.getById(id);
        if (volunteer == null) {
            return Result.error("志愿者不存在");
        }
        volunteer.setPassword(null);

        LambdaQueryWrapper<OrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderItem::getVolunteerId, id);
        wrapper.eq(OrderItem::getItemStatus, 3);
        Long completedCount = orderItemMapper.selectCount(wrapper);

        LambdaQueryWrapper<Review> reviewWrapper = new LambdaQueryWrapper<>();
        reviewWrapper.eq(Review::getVolunteerId, id);
        reviewWrapper.orderByDesc(Review::getCreateTime);
        List<Review> reviews = reviewMapper.selectList(reviewWrapper);

        double averageRating = 0.0;
        if (!reviews.isEmpty()) {
            averageRating = reviews.stream()
                    .mapToInt(Review::getRating)
                    .average()
                    .orElse(0.0);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("volunteer", volunteer);
        result.put("completedCount", completedCount);
        result.put("averageRating", averageRating);
        result.put("reviews", reviews);
        
        return Result.success(result);
    }

    @DeleteMapping("/delete/{id}")
    public Result<Void> deleteVolunteer(@PathVariable Long id) {
        Volunteer volunteer = volunteerService.getById(id);
        if (volunteer == null) {
            return Result.error("志愿者不存在");
        }
        
        boolean success = volunteerService.logicalDeleteVolunteer(id);
        
        if (!success) {
            return Result.error("删除失败");
        }
        return Result.success();
    }
    
    @PostMapping("/status/{id}")

    public Result<Void> updateVolunteerStatus(@PathVariable Long id, @RequestParam Integer status) {
        Volunteer volunteer = volunteerService.getById(id);
        if (volunteer == null) {
            return Result.error("志愿者不存在");
        }
        
        volunteer.setStatus(status);
        volunteerService.updateById(volunteer);
        
        if (status == 0) {
            int releasedCount = volunteerService.releaseVolunteerServices(id);
            log.info("志愿者 {} 被禁用，释放了 {} 个进行中的服务", id, releasedCount);
        }
        
        return Result.success();
    }
}

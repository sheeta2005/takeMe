package com.me.controller.volunteer;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.me.context.BaseContext;
import com.me.dto.PageResultDTO;
import com.me.result.Result;
import com.me.service.OrderService;
import com.me.vo.OrderVO;
import com.me.vo.PageResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "志愿者-订单与服务管理")
@RestController
@RequestMapping("/api/volunteer/order")
@RequiredArgsConstructor
public class VolunteerOrderController {

    private final OrderService orderService;

    @Operation(summary = "查列表")
    @GetMapping("/list")
    public Result<PageResultVO<OrderVO>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String orderNo
    ) {
        Long volunteerId = BaseContext.getLoginId();
        PageResultDTO pageResultDTO = new PageResultDTO();
        pageResultDTO.setPageNum(pageNum);
        pageResultDTO.setPageSize(pageSize);

        IPage<OrderVO> iPage = orderService.getVolunteerOrderList(volunteerId, status, orderNo, pageResultDTO);
        PageResultVO<OrderVO> result = PageResultVO.from(iPage);
        return Result.success(result);
    }

    @Operation(summary = "查可领取订单列表")
    @GetMapping("/available")
    public Result<PageResultVO<OrderVO>> available(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        PageResultDTO pageResultDTO = new PageResultDTO();
        pageResultDTO.setPageNum(pageNum);
        pageResultDTO.setPageSize(pageSize);

        IPage<OrderVO> iPage = orderService.getAvailableOrderList(pageResultDTO);
        PageResultVO<OrderVO> result = PageResultVO.from(iPage);
        return Result.success(result);
    }

    @Operation(summary = "查服务详情")
    @GetMapping("/detail")
    public Result<OrderVO> detail(@RequestParam Long orderId) {
        Long volunteerId = BaseContext.getLoginId();
        OrderVO orderVO = orderService.getVolunteerOrderDetail(volunteerId, orderId);
        return Result.success(orderVO);
    }

    @Operation(summary = "确认接单")
    @PostMapping("/confirm")
    public Result<Void> confirm(@RequestParam Long orderItemId) {
        Long volunteerId = BaseContext.getLoginId();
        orderService.volunteerConfirmOrder(volunteerId, orderItemId);
        return Result.success();
    }

    @Operation(summary = "放弃服务")
    @PostMapping("/abandon")
    public Result<Void> abandon(@RequestParam Long orderItemId) {
        Long volunteerId = BaseContext.getLoginId();
        orderService.volunteerAbandonOrder(volunteerId, orderItemId);
        return Result.success();
    }

    @Operation(summary = "完成服务")
    @PostMapping("/complete")
    public Result<Void> complete(@RequestParam Long orderItemId) {
        Long volunteerId = BaseContext.getLoginId();
        orderService.volunteerCompleteOrder(volunteerId, orderItemId);
        return Result.success();
    }

    @Operation(summary = "开始服务")
    @PostMapping("/start")
    public Result<Void> start(@RequestParam Long orderItemId) {
        Long volunteerId = BaseContext.getLoginId();
        orderService.volunteerStartService(volunteerId, orderItemId);
        return Result.success();
    }
}

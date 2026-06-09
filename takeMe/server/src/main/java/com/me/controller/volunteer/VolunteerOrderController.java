package com.me.controller.volunteer;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.me.context.BaseContext;
import com.me.dto.PageResultDTO;
import com.me.service.OrderService;
import com.me.vo.OrderVO;
import com.me.vo.PageResultVO;
import com.me.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/volunteer/order")
@RequiredArgsConstructor
public class VolunteerOrderController {

    private final OrderService orderService;

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

    @GetMapping("/detail")
    public Result<OrderVO> detail(@RequestParam Long orderId) {
        Long volunteerId = BaseContext.getLoginId();
        OrderVO orderVO = orderService.getVolunteerOrderDetail(volunteerId, orderId);
        return Result.success(orderVO);
    }

    @PostMapping("/confirm")
    public Result<Void> confirm(@RequestParam Long orderItemId) {
        Long volunteerId = BaseContext.getLoginId();
        orderService.volunteerConfirmOrder(volunteerId, orderItemId);
        return Result.success();
    }

    @PostMapping("/abandon")
    public Result<Void> abandon(@RequestParam Long orderItemId) {
        Long volunteerId = BaseContext.getLoginId();
        orderService.volunteerAbandonOrder(volunteerId, orderItemId);
        return Result.success();
    }

    @PostMapping("/complete")
    public Result<Void> complete(@RequestParam Long orderItemId) {
        Long volunteerId = BaseContext.getLoginId();
        orderService.volunteerCompleteOrder(volunteerId, orderItemId);
        return Result.success();
    }

    @PostMapping("/start")
    public Result<Void> start(@RequestParam Long orderItemId) {
        Long volunteerId = BaseContext.getLoginId();
        orderService.volunteerStartService(volunteerId, orderItemId);
        return Result.success();
    }
}

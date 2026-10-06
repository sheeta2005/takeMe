package com.me.handler;

import com.me.exception.OrderBusinessException;
import com.me.exception.ShoppingCartBusinessException;
import com.me.utils.ServiceTimeValidator;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class BusinessExceptionHandlingTest {

    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new ErrorController())
            .setControllerAdvice(new GlobalExceptionHandler()).build();

    @Test
    void businessFailurePreservesExistingClientMessage() throws Exception {
        mvc.perform(get("/test/order")).andExpect(status().isOk())
                .andExpect(jsonPath("$.msg").value("订单不可操作"));
        mvc.perform(get("/test/cart")).andExpect(status().isOk())
                .andExpect(jsonPath("$.msg").value("购物车为空"));
    }

    @Test
    void unexpectedRuntimeFailureReturns500WithoutInternalDetails() throws Exception {
        mvc.perform(get("/test/system")).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.msg").value("系统繁忙，请稍后重试"));
    }

    @Test
    void invalidArgumentStillReturns400() throws Exception {
        mvc.perform(get("/test/argument")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void invalidAppointmentFormatsBecomeOrderBusinessFailures() {
        assertEquals("服务时间格式错误", assertThrows(OrderBusinessException.class,
                () -> ServiceTimeValidator.validateCanAcceptOrder("非法日期", "12:00")).getMessage());
        assertEquals("服务时间格式错误", assertThrows(OrderBusinessException.class,
                () -> ServiceTimeValidator.validateCanStartService("2026-10-05", "非法时间")).getMessage());
    }

    @RestController
    static class ErrorController {
        @GetMapping("/test/order")
        public void order() { throw new OrderBusinessException("订单不可操作"); }

        @GetMapping("/test/cart")
        public void cart() { throw new ShoppingCartBusinessException("购物车为空"); }

        @GetMapping("/test/system")
        public void system() { throw new NullPointerException("内部连接信息"); }

        @GetMapping("/test/argument")
        public void argument() { throw new IllegalArgumentException("分页超出限制"); }
    }
}

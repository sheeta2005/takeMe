package com.me.service.Impl;

import com.me.context.BaseContext;
import com.me.controller.volunteer.VolunteerController;
import com.me.entity.Volunteer;
import com.me.mapper.ApprovalMapper;
import com.me.mapper.OrderItemMapper;
import com.me.redis.annotation.RedisCache;
import com.me.service.VolunteerService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProfileSecurityTest {

    @Test
    void profileUpdateCannotChangePointsOrAccountFields() {
        VolunteerService service = mock(VolunteerService.class);
        Volunteer existing = new Volunteer();
        existing.setId(1L);
        when(service.getById(1L)).thenReturn(existing);
        when(service.updateById(any(Volunteer.class))).thenReturn(true);
        Volunteer request = new Volunteer();
        request.setId(999L);
        request.setPoints(999999);
        request.setStatus(1);
        request.setUsername("other");
        request.setRealName("测试姓名");
        BaseContext.setLoginId(1L);
        try {
            new VolunteerController(service, mock(OrderItemMapper.class), mock(ApprovalMapper.class))
                    .update(request);
            ArgumentCaptor<Volunteer> saved = ArgumentCaptor.forClass(Volunteer.class);
            verify(service).updateById(saved.capture());
            assertEquals(1L, saved.getValue().getId());
            assertEquals("测试姓名", saved.getValue().getRealName());
            assertNull(saved.getValue().getPoints());
            assertNull(saved.getValue().getStatus());
            assertNull(saved.getValue().getUsername());
        } finally {
            BaseContext.clear();
        }
    }

    @Test
    void adminProfileMustNotUseStaleCache() throws Exception {
        RedisCache cache = AdminServiceImpl.class.getMethod("getAdminInfo", Long.class)
                .getAnnotation(RedisCache.class);
        assertNull(cache);
    }
}

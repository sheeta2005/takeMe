package com.me.service;

import com.me.entity.Volunteer;
import com.me.mapper.OrderItemMapper;
import com.me.mapper.VolunteerLeaveMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class VolunteerAvailabilityServiceTest {
    private final VolunteerLeaveMapper leaves = mock(VolunteerLeaveMapper.class);
    private final OrderItemMapper items = mock(OrderItemMapper.class);
    private final VolunteerAvailabilityService service = new VolunteerAvailabilityService(leaves, items);

    private Volunteer volunteer(String days) {
        Volunteer volunteer = new Volunteer();
        volunteer.setId(10014L);
        volunteer.setStatus(1);
        volunteer.setWorkStatus(0);
        volunteer.setServiceDays(days);
        return volunteer;
    }

    // 复现周二排班但遗留 work_status=0 的实际问题。
    @Test
    void tuesdayScheduleOverridesStaleRestStatus() {
        assertEquals(1, service.workStatusAt(volunteer("2,3,4"), LocalDateTime.of(2026, 10, 13, 12, 0)));
        assertEquals(0, service.workStatusAt(volunteer("1,3,4"), LocalDateTime.of(2026, 10, 13, 12, 0)));
    }

    @Test
    void sundayUsesZeroAndMissingScheduleMeansRest() {
        assertEquals(1, service.workStatusAt(volunteer("0,2"), LocalDateTime.of(2026, 10, 18, 12, 0)));
        assertEquals(0, service.workStatusAt(volunteer(null), LocalDateTime.of(2026, 10, 18, 12, 0)));
    }

    @Test
    void approvedLeaveOverridesScheduleAndRealServiceDeterminesBusyStatus() {
        when(leaves.countApprovedAt(anyLong(), any(LocalDateTime.class))).thenReturn(1L);
        assertEquals(3, service.workStatusAt(volunteer("2,3,4"), LocalDateTime.of(2026, 10, 13, 12, 0)));
        when(leaves.countApprovedAt(anyLong(), any(LocalDateTime.class))).thenReturn(0L);
        when(items.countForVolunteerStatus(anyLong(), eq(2))).thenReturn(1L);
        assertEquals(2, service.workStatusAt(volunteer("2,3,4"), LocalDateTime.of(2026, 10, 13, 12, 0)));
    }
}

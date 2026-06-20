package com.me.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ServiceTimeValidator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public static void validateCanViewService(Long orderItemId, String serviceDate, String serviceTime) {
        if (serviceDate == null || serviceTime == null) {
            return;
        }

        try {
            LocalDate serviceLocalDate = LocalDate.parse(serviceDate, DATE_FORMATTER);
            LocalTime serviceLocalTime = LocalTime.parse(serviceTime, TIME_FORMATTER);

            LocalDateTime serviceDateTime = LocalDateTime.of(serviceLocalDate, serviceLocalTime);
            LocalDateTime now = LocalDateTime.now();

            LocalDateTime earliestVisibleTime = serviceDateTime.minusHours(4);

            if (now.isBefore(earliestVisibleTime)) {
                throw new RuntimeException("服务尚未开放接取，预约时间为" + serviceDate + " " + serviceTime +
                        "，将于" + earliestVisibleTime.format(DATE_FORMATTER) + " " +
                        earliestVisibleTime.format(TIME_FORMATTER) + "后开放");
            }
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException("服务时间格式错误");
        }
    }

    public static void validateCanStartService(String serviceDate, String serviceTime) {
        if (serviceDate == null || serviceTime == null) {
            return;
        }

        try {
            LocalDate serviceLocalDate = LocalDate.parse(serviceDate, DATE_FORMATTER);
            LocalTime serviceLocalTime = LocalTime.parse(serviceTime, TIME_FORMATTER);

            LocalDateTime serviceDateTime = LocalDateTime.of(serviceLocalDate, serviceLocalTime);
            LocalDateTime now = LocalDateTime.now();

            LocalDateTime earliestStartTime = serviceDateTime.minusHours(2);
            LocalDateTime latestStartTime = serviceDateTime.plusHours(1);

            if (now.isBefore(earliestStartTime)) {
                throw new RuntimeException("服务尚未到达可开始时间，预约时间为" + serviceDate + " " + serviceTime +
                        "，最早可于" + earliestStartTime.format(DATE_FORMATTER) + " " +
                        earliestStartTime.format(TIME_FORMATTER) + "开始服务");
            }

            if (now.isAfter(latestStartTime)) {
                throw new RuntimeException("已超过预约时间，请联系用户协商修改服务时间");
            }
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException("服务时间格式错误");
        }
    }
}

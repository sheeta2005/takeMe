package com.me.utils;

import com.me.exception.OrderBusinessException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class ServiceTimeValidator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public static void validateCanAcceptOrder(String serviceDate, String serviceTime) {
        if (serviceDate == null || serviceTime == null) {
            return;
        }

        try {
            LocalDate serviceLocalDate = LocalDate.parse(serviceDate, DATE_FORMATTER);
            LocalTime serviceLocalTime = LocalTime.parse(serviceTime, TIME_FORMATTER);

            LocalDateTime serviceDateTime = LocalDateTime.of(serviceLocalDate, serviceLocalTime);
            LocalDateTime now = LocalDateTime.now();

            LocalDateTime earliestAcceptTime = serviceDateTime.minusHours(4);
            LocalDateTime latestAcceptTime = serviceDateTime.minusHours(1);

            if (now.isBefore(earliestAcceptTime)) {
                throw new OrderBusinessException("服务尚未开放接取，预约时间为" + serviceDate + " " + serviceTime +
                        "，将于" + earliestAcceptTime.format(DATE_FORMATTER) + " " +
                        earliestAcceptTime.format(TIME_FORMATTER) + "后开放接单");
            }

            if (now.isAfter(latestAcceptTime)) {
                throw new OrderBusinessException("已超过接单截止时间，预约时间为" + serviceDate + " " + serviceTime +
                        "，接单截止时间为" + latestAcceptTime.format(DATE_FORMATTER) + " " +
                        latestAcceptTime.format(TIME_FORMATTER));
            }
        } catch (DateTimeParseException e) {
            // 日期解析错误统一转为业务提示，不能将解析器内部异常返回客户端。
            throw new OrderBusinessException("服务时间格式错误");
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

            LocalDateTime earliestStartTime = serviceDateTime.minusHours(1);

            if (now.isBefore(earliestStartTime)) {
                throw new OrderBusinessException("服务尚未到达可开始时间，预约时间为" + serviceDate + " " + serviceTime +
                        "，最早可于" + earliestStartTime.format(DATE_FORMATTER) + " " +
                        earliestStartTime.format(TIME_FORMATTER) + "开始服务");
            }
        } catch (DateTimeParseException e) {
            // 不捕获服务时间窗口校验抛出的订单业务异常。
            throw new OrderBusinessException("服务时间格式错误");
        }
    }

    public static boolean isWithinVisibleRange(String serviceDate, String serviceTime) {
        if (serviceDate == null || serviceTime == null) {
            return true;
        }

        try {
            LocalDate serviceLocalDate = LocalDate.parse(serviceDate, DATE_FORMATTER);
            LocalTime serviceLocalTime = LocalTime.parse(serviceTime, TIME_FORMATTER);

            LocalDateTime serviceDateTime = LocalDateTime.of(serviceLocalDate, serviceLocalTime);
            LocalDateTime now = LocalDateTime.now();

            LocalDateTime earliestVisibleTime = serviceDateTime.minusHours(4);

            return !now.isBefore(earliestVisibleTime);
        } catch (Exception e) {
            return false;
        }
    }
}

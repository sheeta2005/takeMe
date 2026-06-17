package com.me.aspect;

import com.me.annotation.BizLog;
import com.me.context.BaseContext;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
@Slf4j
public class BizLogAspect {

    @Around("@annotation(com.me.annotation.BizLog)")
    public Object logBizOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        BizLog bizLog = method.getAnnotation(BizLog.class);

        String bizDesc = bizLog.value();
        if (bizDesc.isEmpty()) {
            bizDesc = method.getName();
        }

        Long userId = BaseContext.getLoginId();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = method.getName();

        long startTime = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();

            long costTime = System.currentTimeMillis() - startTime;

            StringBuilder logMsg = new StringBuilder();
            logMsg.append("【业务成功】{} | 用户ID: {} | 类: {} | 方法: {} | 耗时: {}ms");

            if (bizLog.logParams()) {
                logMsg.append(" | 参数: {}");
            }

            log.info(logMsg.toString(),
                    bizDesc,
                    userId,
                    className,
                    methodName,
                    costTime,
                    bizLog.logParams() ? joinPoint.getArgs() : "");

            return result;
        } catch (Exception e) {
            long costTime = System.currentTimeMillis() - startTime;

            log.error("【业务失败】{} | 用户ID: {} | 类: {} | 方法: {} | 耗时: {}ms | 异常: {}",
                    bizDesc,
                    userId,
                    className,
                    methodName,
                    costTime,
                    e.getMessage(),
                    e);

            throw e;
        }
    }
}

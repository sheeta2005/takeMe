package com.me.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface BizLog {
    //业务操作名称
    String value() default ""; // 业务描述
    //是否记录请求参数
    boolean logParams() default true; // 是否记录参数
    //是否记录返回结果
    boolean logResult() default false; // 是否记录返回值
}

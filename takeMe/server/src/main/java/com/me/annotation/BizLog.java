package com.me.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface BizLog {
    String value() default ""; // 业务描述
    boolean logParams() default true; // 是否记录参数
    boolean logResult() default false; // 是否记录返回值
}

package com.me.redis.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    //限流键前缀
    String prefix();

    //允许请求次数
    int count() default 10;

    //限流时间窗口（秒）
    int period() default 60;
}

package com.me.redis.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RedisCache {

    //缓存键前缀
    String prefix();

    //缓存有效期（分钟）
    long expire() default 120;

    //空值缓存有效期（分钟）
    long nullExpire() default 2;

    //参与缓存键的参数下标
    int[] keyArgs() default {};
}

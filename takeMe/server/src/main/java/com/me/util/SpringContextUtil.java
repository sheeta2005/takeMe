package com.me.util;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class SpringContextUtil implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        applicationContext = context;
    }

    public static <T> T getBean(Class<T> clazz) {
        if (applicationContext == null) {
            throw new IllegalStateException("ApplicationContext未初始化");
        }
        return applicationContext.getBean(clazz);
    }

    public static Object getBean(String name) {
        if (applicationContext == null) {
            throw new IllegalStateException("ApplicationContext未初始化");
        }
        return applicationContext.getBean(name);
    }
}

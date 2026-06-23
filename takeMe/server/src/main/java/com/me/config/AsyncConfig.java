package com.me.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
//自定义异步线程池配置

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean("messageTaskExecutor")
    public Executor messageTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);// 阻塞队列容量：线程全部忙碌时，新任务存入队列，最多存100个
        executor.setThreadNamePrefix("message-task-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 拒绝策略：队列、最大线程全部占满时，新任务交给调用者主线程执行
        executor.initialize();
        return executor;
    }
}

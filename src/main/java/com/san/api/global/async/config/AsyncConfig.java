package com.san.api.global.async.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 비동기 실행 활성화 및 전용 스레드 풀 설정.
 *
 * @Async("asyncJobExecutor") 어노테이션을 사용하는 비동기 작업이 실행되려면 반드시 필요.
 */
@EnableAsync
@Configuration
public class AsyncConfig {

    @Bean(name = "asyncJobExecutor")
    public Executor asyncJobExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("async-job-");
        executor.initialize();
        return executor;
    }
}

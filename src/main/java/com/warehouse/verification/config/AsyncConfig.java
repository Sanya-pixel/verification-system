package com.warehouse.verification.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "bulkUploadExecutor")
    public Executor bulkUploadExecutor() {
        ThreadPoolTaskExecutor executionPool = new ThreadPoolTaskExecutor();
        executionPool.setCorePoolSize(10);
        executionPool.setMaxPoolSize(25);
        executionPool.setQueueCapacity(500);
        executionPool.setThreadNamePrefix("BulkUploadWorker-");
        executionPool.initialize();
        return executionPool;
    }
}

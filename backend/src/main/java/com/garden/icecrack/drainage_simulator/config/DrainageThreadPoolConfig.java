package com.garden.icecrack.drainage_simulator.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class DrainageThreadPoolConfig {

    @Bean("drainageExecutor")
    public Executor drainageExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("swe-");
        executor.setRejectedExecutionHandler((r, e) -> {
            throw new RuntimeException("Drainage simulation queue full");
        });
        executor.initialize();
        return executor;
    }
}

package com.urlshortener.api.config;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Bounded pool for click-event writes so a burst of redirects can't spawn unbounded threads;
 * excess tasks fall onto the caller (CallerRunsPolicy) rather than being dropped, trading a
 * brief redirect-thread stall for never losing a click event.
 */
@Configuration
public class AsyncConfig {

    public static final String CLICK_EVENT_EXECUTOR = "clickEventExecutor";

    @Bean(CLICK_EVENT_EXECUTOR)
    public Executor clickEventExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("click-event-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}

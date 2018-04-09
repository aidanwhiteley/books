package com.aidanwhiteley.books.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class ThreadPoolConfig {

    @Value("${books.thread.pool1.coresize}")
    private int booksThreadPool1CoreSize;

    @Value("${books.thread.pool1.maxsize}")
    private int booksThreadPool1MaxSize;

    @Value("${books.thread.pool1.queue.capacity}")
    private int booksThreadPool1QueueCapacity;

    @Value("${books.thread.pool1.thread.prefix}")
    private String booksThreadPool1ThreadPrefix;

    @Bean(name = "threadPoolExecutor")
    @Profile(value = "!test")
    public Executor asyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(booksThreadPool1CoreSize);
        executor.setMaxPoolSize(booksThreadPool1MaxSize);
        executor.setQueueCapacity(booksThreadPool1QueueCapacity);
        executor.setThreadNamePrefix(booksThreadPool1ThreadPrefix);
        executor.initialize();
        return executor;
    }
}

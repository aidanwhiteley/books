package com.aidanwhiteley.books.repository.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.task.SyncTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@Profile({"test"})
public class ThreadingConfig {

    @Bean(name = "threadPoolExecutor")
    public Executor asyncExecutor() {
        return new SyncTaskExecutor();
    }
}

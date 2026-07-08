package com.aidanwhiteley.books.repository.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.task.SyncTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@Profile({"ci"})
public class ThreadingConfig {

    @Bean(name = {"applicationTaskExecutor", "taskExecutor"})
    public Executor asyncExecutor() {
        return new SyncTaskExecutor();
    }
}

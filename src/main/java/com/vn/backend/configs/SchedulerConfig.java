package com.vn.backend.configs;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
@EnableScheduling
public class SchedulerConfig {

  @Bean(name = "examTaskExecutor")
  public Executor examTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(3);
    executor.setMaxPoolSize(8);
    executor.setQueueCapacity(100);
    executor.setKeepAliveSeconds(60);
    executor.setThreadNamePrefix("exam-scheduler-");
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(60);
    executor.initialize();

    return executor;
  }

}

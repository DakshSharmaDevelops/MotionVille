package org.example.motionville.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class VideoProcessingConfig {
    @Bean
    public ThreadPoolTaskExecutor videoProcessingExecutor() {
        // Encoding is CPU-heavy. One worker prevents several uploads from
        // launching many FFmpeg processes at once; the queue is bounded too.
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("video-processing-");
        return executor;
    }
}

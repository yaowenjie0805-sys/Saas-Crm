package com.yao.crm.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * 报表导出专用线程池
     * 优化：根据CPU核心数动态调整，增加队列容量
     */
    @Bean("reportExportExecutor")
    public ThreadPoolTaskExecutor reportExportExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(Runtime.getRuntime().availableProcessors() * 2);
        executor.setMaxPoolSize(Runtime.getRuntime().availableProcessors() * 4);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("report-export-");
        executor.setTaskDecorator(new MdcTaskDecorator());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setKeepAliveSeconds(60);
        executor.initialize();
        return executor;
    }

    /**
     * 审计导出专用线程池
     */
    @Bean("auditExportExecutor")
    public ThreadPoolTaskExecutor auditExportExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(Runtime.getRuntime().availableProcessors() * 2);
        executor.setMaxPoolSize(Runtime.getRuntime().availableProcessors() * 3);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("audit-export-");
        executor.setTaskDecorator(new MdcTaskDecorator());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setKeepAliveSeconds(60);
        executor.initialize();
        return executor;
    }

    /**
     * 通用异步处理线程池
     */
    @Bean("taskExecutor")
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(Runtime.getRuntime().availableProcessors());
        executor.setMaxPoolSize(Runtime.getRuntime().availableProcessors() * 2);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("async-task-");
        executor.setTaskDecorator(new MdcTaskDecorator());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setKeepAliveSeconds(30);
        executor.initialize();
        return executor;
    }
}

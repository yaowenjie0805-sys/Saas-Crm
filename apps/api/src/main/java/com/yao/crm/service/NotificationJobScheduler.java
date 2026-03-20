package com.yao.crm.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class NotificationJobScheduler {

    private final NotificationJobService notificationJobService;
    private volatile LocalDateTime lastRunAt;
    private volatile Integer lastProcessed;
    private volatile String lastError;

    public NotificationJobScheduler(NotificationJobService notificationJobService) {
        this.notificationJobService = notificationJobService;
    }

    @Scheduled(fixedDelayString = "${integration.notifications.scan-ms:20000}")
    public void runQueue() {
        lastRunAt = LocalDateTime.now();
        try {
            lastProcessed = notificationJobService.processQueue();
            lastError = null;
        } catch (Exception ex) {
            lastError = ex.getMessage();
            throw ex;
        }
    }

    public LocalDateTime getLastRunAt() {
        return lastRunAt;
    }

    public Integer getLastProcessed() {
        return lastProcessed;
    }

    public String getLastError() {
        return lastError;
    }
}

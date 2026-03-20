package com.yao.crm.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class AsyncProcessingService {

    private <T> CompletableFuture<T> failedFutureCompat(Exception exception) {
        CompletableFuture<T> future = new CompletableFuture<>();
        future.completeExceptionally(exception);
        return future;
    }

    @Async("reportExportExecutor")
    public CompletableFuture<String> processLargeReportExport(String jobId, Object exportParams) {
        try {
            Thread.sleep(5000);
            return CompletableFuture.completedFuture("EXPORT_COMPLETED_" + jobId);
        } catch (Exception e) {
            return failedFutureCompat(e);
        }
    }

    @Async("auditExportExecutor")
    public CompletableFuture<String> processAuditExport(String jobId, Object auditParams) {
        try {
            Thread.sleep(3000);
            return CompletableFuture.completedFuture("AUDIT_EXPORT_COMPLETED_" + jobId);
        } catch (Exception e) {
            return failedFutureCompat(e);
        }
    }

    @Async("taskExecutor")
    public CompletableFuture<Boolean> sendAsyncNotification(String userId, String message) {
        try {
            Thread.sleep(1000);
            return CompletableFuture.completedFuture(true);
        } catch (Exception e) {
            return CompletableFuture.completedFuture(false);
        }
    }

    @Async("taskExecutor")
    public CompletableFuture<Integer> batchProcessData(Object[] dataItems) {
        try {
            int processedCount = 0;
            for (Object item : dataItems) {
                Thread.sleep(100);
                processedCount += item == null ? 0 : 1;
            }
            return CompletableFuture.completedFuture(processedCount);
        } catch (Exception e) {
            return failedFutureCompat(e);
        }
    }
}

package com.yao.crm.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 异步处理服务
 */
@Service
public class AsyncProcessingService {

    private static final Logger log = LoggerFactory.getLogger(AsyncProcessingService.class);
    private static final String DEFAULT_TENANT_ID = "";
    private static final String DEFAULT_REQUESTED_BY = "system";
    private static final String DEFAULT_NOTIFICATION_TENANT_ID = "tenant_default";

    private final ReportExportJobService reportExportJobService;
    private final AuditExportJobService auditExportJobService;
    private final NotificationDispatchService notificationDispatchService;

    public AsyncProcessingService(ReportExportJobService reportExportJobService,
                                  AuditExportJobService auditExportJobService,
                                  NotificationDispatchService notificationDispatchService) {
        this.reportExportJobService = reportExportJobService;
        this.auditExportJobService = auditExportJobService;
        this.notificationDispatchService = notificationDispatchService;
    }

    @Async("reportExportExecutor")
    public CompletableFuture<String> processLargeReportExport(String jobId, Object exportParams) {
        log.info("Processing large report export: jobId={}", jobId);
        try {
            Map<String, Object> params = asMap(exportParams);
            String tenantId = getString(params, "tenantId", DEFAULT_TENANT_ID);
            String requestedBy = getString(params, "requestedBy", DEFAULT_REQUESTED_BY);
            if (tenantId.isEmpty()) {
                throw new IllegalArgumentException("tenant_id_required");
            }
            String role = getString(params, "role", "");
            String owner = getString(params, "owner", "");
            String department = getString(params, "department", "");
            String timezone = getString(params, "timezone", "Asia/Shanghai");
            String currency = getString(params, "currency", "CNY");
            String language = getString(params, "language", "en");
            LocalDate fromDate = parseLocalDate(params.get("fromDate"));
            LocalDate toDate = parseLocalDate(params.get("toDate"));

            Map<String, Object> status = reportExportJobService.submitByTenant(
                    requestedBy, tenantId, role, fromDate, toDate, owner, department, timezone, currency, language
            );
            return CompletableFuture.completedFuture(String.valueOf(status.getOrDefault("jobId", jobId)));
        } catch (Exception ex) {
            log.error("Failed to process report export: jobId={}", jobId, ex);
            return CompletableFuture.failedFuture(ex);
        }
    }

    @Async("auditExportExecutor")
    public CompletableFuture<String> processAuditExport(String jobId, Object auditParams) {
        log.info("Processing audit export: jobId={}", jobId);
        try {
            Map<String, Object> params = asMap(auditParams);
            String tenantId = getString(params, "tenantId", DEFAULT_TENANT_ID);
            String requestedBy = getString(params, "requestedBy", DEFAULT_REQUESTED_BY);
            if (tenantId.isEmpty()) {
                throw new IllegalArgumentException("tenant_id_required");
            }
            String role = getString(params, "role", "");
            String username = getString(params, "username", "");
            String action = getString(params, "action", "");
            String language = getString(params, "language", "en");
            LocalDateTime fromTime = parseLocalDateTime(params.get("fromTime"));
            LocalDateTime toTime = parseLocalDateTime(params.get("toTime"));

            Map<String, Object> status = auditExportJobService.submit(
                    requestedBy, tenantId, role, username, action, fromTime, toTime, language
            );
            return CompletableFuture.completedFuture(String.valueOf(status.getOrDefault("jobId", jobId)));
        } catch (Exception ex) {
            log.error("Failed to process audit export: jobId={}", jobId, ex);
            return CompletableFuture.failedFuture(ex);
        }
    }

    @Async("taskExecutor")
    public CompletableFuture<Boolean> sendAsyncNotification(String userId, String message) {
        log.info("Sending async notification to user: userId={}, message={}", userId, message);
        try {
            Map<String, Object> payload = new LinkedHashMap<String, Object>();
            payload.put("userId", userId == null ? "" : userId);
            payload.put("message", message == null ? "" : message);
            notificationDispatchService.sendNotification(DEFAULT_NOTIFICATION_TENANT_ID, "async_notification", payload);
            return CompletableFuture.completedFuture(true);
        } catch (Exception ex) {
            log.error("Failed to send async notification: userId={}", userId, ex);
            return CompletableFuture.failedFuture(ex);
        }
    }

    @Async("taskExecutor")
    public CompletableFuture<Integer> batchProcessData(Object[] dataItems) {
        log.info("Batch processing data: itemCount={}", dataItems != null ? dataItems.length : 0);
        int processedCount = 0;
        if (dataItems != null) {
            for (Object item : dataItems) {
                if (item == null) {
                    continue;
                }
                processedCount += 1;
            }
        }
        return CompletableFuture.completedFuture(processedCount);
    }

    private Map<String, Object> asMap(Object raw) {
        if (raw instanceof Map<?, ?>) {
            @SuppressWarnings("unchecked")
            Map<String, Object> casted = (Map<String, Object>) raw;
            return casted;
        }
        return new LinkedHashMap<String, Object>();
    }

    private String getString(Map<String, Object> map, String key, String fallback) {
        Object raw = map.get(key);
        if (raw == null) return fallback;
        String value = String.valueOf(raw).trim();
        return value.isEmpty() ? fallback : value;
    }

    private LocalDate parseLocalDate(Object raw) {
        if (raw == null) return null;
        if (raw instanceof LocalDate) return (LocalDate) raw;
        String text = String.valueOf(raw).trim();
        if (text.isEmpty()) return null;
        return LocalDate.parse(text);
    }

    private LocalDateTime parseLocalDateTime(Object raw) {
        if (raw == null) return null;
        if (raw instanceof LocalDateTime) return (LocalDateTime) raw;
        String text = String.valueOf(raw).trim();
        if (text.isEmpty()) return null;
        return LocalDateTime.parse(text);
    }
}

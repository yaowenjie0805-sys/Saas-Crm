package com.yao.crm.controller;

import com.yao.crm.entity.ApprovalTask;
import com.yao.crm.entity.LeadImportJob;
import com.yao.crm.entity.NotificationJob;
import com.yao.crm.repository.ApprovalTaskRepository;
import com.yao.crm.repository.LeadImportJobRepository;
import com.yao.crm.repository.NotificationJobRepository;
import com.yao.crm.repository.TenantRepository;
import com.yao.crm.service.I18nService;
import com.yao.crm.service.NotificationJobScheduler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ops")
public class V1OpsController extends BaseApiController {

    private final DataSource dataSource;
    private final NotificationJobScheduler notificationJobScheduler;
    private final ApprovalTaskRepository approvalTaskRepository;
    private final NotificationJobRepository notificationJobRepository;
    private final LeadImportJobRepository leadImportJobRepository;
    private final TenantRepository tenantRepository;
    private final String webhookProviders;

    public V1OpsController(DataSource dataSource,
                           NotificationJobScheduler notificationJobScheduler,
                           ApprovalTaskRepository approvalTaskRepository,
                           NotificationJobRepository notificationJobRepository,
                           LeadImportJobRepository leadImportJobRepository,
                           TenantRepository tenantRepository,
                           @Value("${integration.webhooks.providers:}") String webhookProviders,
                           I18nService i18nService) {
        super(i18nService);
        this.dataSource = dataSource;
        this.notificationJobScheduler = notificationJobScheduler;
        this.approvalTaskRepository = approvalTaskRepository;
        this.notificationJobRepository = notificationJobRepository;
        this.leadImportJobRepository = leadImportJobRepository;
        this.tenantRepository = tenantRepository;
        this.webhookProviders = webhookProviders == null ? "" : webhookProviders;
    }

    @GetMapping("/health")
    public ResponseEntity<?> health(HttpServletRequest request) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "ANALYST")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        String tenantId = currentTenant(request);
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("requestId", traceId(request));
        out.put("tenantId", tenantId);
        out.put("time", LocalDateTime.now());
        out.put("database", databaseHealth());
        out.put("notificationScheduler", schedulerHealth());
        out.put("webhookProviders", webhookProviders);
        out.put("webhookConfigured", !webhookProviders.trim().isEmpty());
        out.put("status", "UP");
        return ResponseEntity.ok(successWithFields(request, "ops_health_loaded", out));
    }

    @GetMapping("/metrics/summary")
    public ResponseEntity<?> metricsSummary(HttpServletRequest request) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "ANALYST")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        String tenantId = currentTenant(request);
        List<ApprovalTask> tasks = approvalTaskRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);
        List<NotificationJob> jobs = notificationJobRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);
        long approvalBacklog = tasks.stream().filter(t -> "PENDING".equalsIgnoreCase(t.getStatus()) || "WAITING".equalsIgnoreCase(t.getStatus())).count();
        long approvalOverdue = tasks.stream().filter(t -> ("PENDING".equalsIgnoreCase(t.getStatus()) || "WAITING".equalsIgnoreCase(t.getStatus())) && t.getDeadlineAt() != null && t.getDeadlineAt().isBefore(LocalDateTime.now())).count();
        long notifySuccess = jobs.stream().filter(j -> "SUCCESS".equalsIgnoreCase(j.getStatus())).count();
        long notifyFailed = jobs.stream().filter(j -> "FAILED".equalsIgnoreCase(j.getStatus())).count();
        long notifyRetry = jobs.stream().filter(j -> "RETRY".equalsIgnoreCase(j.getStatus())).count();
        long notifyTotal = notifySuccess + notifyFailed + notifyRetry + jobs.stream().filter(j -> "PENDING".equalsIgnoreCase(j.getStatus()) || "RUNNING".equalsIgnoreCase(j.getStatus())).count();
        double successRate = notifyTotal == 0 ? 1.0 : ((double) notifySuccess / (double) notifyTotal);
        Map<String, Object> retryBuckets = buildRetryDistribution(jobs);

        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("requestId", traceId(request));
        out.put("tenantId", tenantId);
        out.put("approvalBacklog", approvalBacklog);
        out.put("approvalOverdue", approvalOverdue);
        out.put("notificationTotal", notifyTotal);
        out.put("notificationSuccess", notifySuccess);
        out.put("notificationFailed", notifyFailed);
        out.put("notificationRetry", notifyRetry);
        out.put("notificationSuccessRate", successRate);
        out.put("notificationRetryDistribution", retryBuckets);
        out.put("importMetrics", buildImportMetrics(tenantId));
        out.put("scheduler", schedulerHealth());
        return ResponseEntity.ok(successWithFields(request, "ops_metrics_loaded", out));
    }

    private Map<String, Object> buildImportMetrics(String tenantId) {
        String timezone = tenantRepository.findById(tenantId).map(t -> t.getTimezone()).orElse("");
        ZoneId zoneId;
        try {
            zoneId = (timezone == null || timezone.trim().isEmpty()) ? ZoneId.systemDefault() : ZoneId.of(timezone.trim());
        } catch (Exception ex) {
            zoneId = ZoneId.systemDefault();
        }
        LocalDateTime now = LocalDateTime.now();
        ZonedDateTime zoneNow = ZonedDateTime.now(zoneId);
        LocalDateTime from = zoneNow.minusHours(24).withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
        List<LeadImportJob> allJobs = leadImportJobRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);
        List<LeadImportJob> jobs = new ArrayList<LeadImportJob>();
        for (LeadImportJob row : allJobs) {
            if (row.getCreatedAt() != null && !row.getCreatedAt().isBefore(from)) {
                jobs.add(row);
            }
        }

        long total = jobs.size();
        long running = jobs.stream().filter(j -> "PENDING".equalsIgnoreCase(j.getStatus()) || "RUNNING".equalsIgnoreCase(j.getStatus())).count();
        long success = jobs.stream().filter(j -> "SUCCESS".equalsIgnoreCase(j.getStatus())).count();
        long partial = jobs.stream().filter(j -> "PARTIAL_SUCCESS".equalsIgnoreCase(j.getStatus())).count();
        long failed = jobs.stream().filter(j -> "FAILED".equalsIgnoreCase(j.getStatus())).count();
        long canceled = jobs.stream().filter(j -> "CANCELED".equalsIgnoreCase(j.getStatus())).count();

        long completed = success + partial + failed + canceled;
        double successRate = completed == 0 ? 1.0 : ((double) (success + partial) / (double) completed);
        double failureRate = completed == 0 ? 0.0 : ((double) failed / (double) completed);
        long processedRows = jobs.stream().mapToLong(j -> j.getProcessedRows() == null ? 0 : j.getProcessedRows()).sum();
        long failedRows = jobs.stream().mapToLong(j -> j.getFailCount() == null ? 0 : j.getFailCount()).sum();

        List<Long> durations = new ArrayList<Long>();
        for (LeadImportJob row : jobs) {
            if (row.getCreatedAt() == null) continue;
            if (!isTerminal(row.getStatus())) continue;
            LocalDateTime end = row.getUpdatedAt() == null ? now : row.getUpdatedAt();
            long ms = Math.max(0L, Duration.between(row.getCreatedAt(), end).toMillis());
            durations.add(ms);
        }
        Collections.sort(durations);
        long avgDurationMs = durations.isEmpty() ? 0L : (long) durations.stream().mapToLong(Long::longValue).average().orElse(0.0);
        long p95DurationMs = durations.isEmpty() ? 0L : durations.get(Math.max(0, (int) Math.ceil(durations.size() * 0.95) - 1));

        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("windowHours", 24);
        out.put("windowFrom", from);
        out.put("windowTo", now);
        out.put("timezone", zoneId.getId());
        out.put("importJobTotal", total);
        out.put("importRunning", running);
        out.put("importSuccess", success);
        out.put("importPartialSuccess", partial);
        out.put("importFailed", failed);
        out.put("importCanceled", canceled);
        out.put("importSuccessRate", successRate);
        out.put("importFailureRate", failureRate);
        out.put("importAvgDurationMs", avgDurationMs);
        out.put("importP95DurationMs", p95DurationMs);
        out.put("importProcessedRows", processedRows);
        out.put("importFailedRows", failedRows);
        return out;
    }

    private boolean isTerminal(String status) {
        return "SUCCESS".equalsIgnoreCase(status)
                || "PARTIAL_SUCCESS".equalsIgnoreCase(status)
                || "FAILED".equalsIgnoreCase(status)
                || "CANCELED".equalsIgnoreCase(status);
    }

    private Map<String, Object> buildRetryDistribution(List<NotificationJob> jobs) {
        long zero = 0;
        long low = 0;
        long high = 0;
        for (NotificationJob job : jobs) {
            int count = job.getRetryCount() == null ? 0 : job.getRetryCount();
            if (count <= 0) {
                zero++;
            } else if (count <= 2) {
                low++;
            } else {
                high++;
            }
        }
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("retry0", zero);
        out.put("retry1to2", low);
        out.put("retry3plus", high);
        return out;
    }

    private Map<String, Object> databaseHealth() {
        try (Connection ignored = dataSource.getConnection()) {
            return Collections.<String, Object>singletonMap("status", "UP");
        } catch (Exception ex) {
            Map<String, Object> out = new LinkedHashMap<String, Object>();
            out.put("status", "DOWN");
            out.put("message", ex.getMessage());
            return out;
        }
    }

    private Map<String, Object> schedulerHealth() {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("lastRunAt", notificationJobScheduler.getLastRunAt());
        out.put("lastProcessed", notificationJobScheduler.getLastProcessed());
        out.put("lastError", notificationJobScheduler.getLastError());
        out.put("status", notificationJobScheduler.getLastError() == null ? "UP" : "DEGRADED");
        return out;
    }
}

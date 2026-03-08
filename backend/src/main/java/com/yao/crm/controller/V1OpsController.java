package com.yao.crm.controller;

import com.yao.crm.entity.ApprovalTask;
import com.yao.crm.entity.NotificationJob;
import com.yao.crm.repository.ApprovalTaskRepository;
import com.yao.crm.repository.NotificationJobRepository;
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
import java.time.LocalDateTime;
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
    private final String webhookProviders;

    public V1OpsController(DataSource dataSource,
                           NotificationJobScheduler notificationJobScheduler,
                           ApprovalTaskRepository approvalTaskRepository,
                           NotificationJobRepository notificationJobRepository,
                           @Value("${integration.webhooks.providers:}") String webhookProviders,
                           I18nService i18nService) {
        super(i18nService);
        this.dataSource = dataSource;
        this.notificationJobScheduler = notificationJobScheduler;
        this.approvalTaskRepository = approvalTaskRepository;
        this.notificationJobRepository = notificationJobRepository;
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
        return ResponseEntity.ok(out);
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
        out.put("scheduler", schedulerHealth());
        return ResponseEntity.ok(out);
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

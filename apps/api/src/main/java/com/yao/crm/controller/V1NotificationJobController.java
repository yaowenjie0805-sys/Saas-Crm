package com.yao.crm.controller;

import com.yao.crm.dto.request.V1NotificationBatchRetryRequest;
import com.yao.crm.dto.request.V1NotificationRetryByFilterRequest;
import com.yao.crm.entity.NotificationJob;
import com.yao.crm.service.I18nService;
import com.yao.crm.service.NotificationJobService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/integrations/notifications")
public class V1NotificationJobController extends BaseApiController {

    private final NotificationJobService notificationJobService;
    private final int batchMaxSize;

    public V1NotificationJobController(NotificationJobService notificationJobService,
                                       @Value("${integration.notifications.batch-max-size:100}") int batchMaxSize,
                                       I18nService i18nService) {
        super(i18nService);
        this.notificationJobService = notificationJobService;
        this.batchMaxSize = Math.max(1, batchMaxSize);
    }

    @GetMapping("/jobs")
    public ResponseEntity<?> listJobs(HttpServletRequest request,
                                      @RequestParam(defaultValue = "ALL") String status,
                                      @RequestParam(defaultValue = "1") int page,
                                      @RequestParam(defaultValue = "20") int size) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER", "ANALYST")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        String tenantId = normalize(currentTenant(request));
        String normalizedStatus = normalizeStatus(status, "ALL");
        int safePage = Math.max(1, page);
        int safeSize = Math.max(1, size);
        @SuppressWarnings("unchecked")
        Map<String, Object> pageBody = (Map<String, Object>) notificationJobService.listJobsPaged(tenantId, normalizedStatus, safePage, safeSize);
        return ResponseEntity.ok(successWithFields(request, "notification_jobs_listed", pageBody));
    }

    @PostMapping("/jobs/{jobId}/retry")
    public ResponseEntity<?> retry(HttpServletRequest request, @PathVariable String jobId) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        String normalizedJobId = normalize(jobId);
        if (isBlank(normalizedJobId)) {
            return ResponseEntity.badRequest().body(errorBody(request, "bad_request", msg(request, "bad_request"), null));
        }
        String tenantId = normalize(currentTenant(request));
        NotificationJob job = notificationJobService.retry(tenantId, normalizedJobId);
        if (job == null) {
            return ResponseEntity.status(404).body(errorBody(request, "notification_job_not_found", msg(request, "notification_job_not_found"), null));
        }
        if (!"RETRY".equalsIgnoreCase(job.getStatus())) {
            Map<String, Object> details = new LinkedHashMap<String, Object>();
            details.put("jobId", normalizedJobId);
            details.put("from", normalizeStatus(job.getStatus(), ""));
            details.put("to", "RETRY");
            return ResponseEntity.status(409).body(errorBody(request, "notification_job_status_transition_invalid", msg(request, "notification_job_status_transition_invalid"), details));
        }
        return ResponseEntity.ok(successWithFields(request, "notification_job_retried", Collections.<String, Object>singletonMap("jobId", job.getId())));
    }

    @PostMapping("/jobs/batch-retry")
    public ResponseEntity<?> batchRetry(HttpServletRequest request, @Valid @RequestBody(required = false) V1NotificationBatchRetryRequest payload) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        List<String> normalizedJobIds = normalizeJobIds(payload == null ? null : payload.getJobIds());
        int requested = normalizedJobIds.size();
        if (requested > batchMaxSize) {
            Map<String, Object> details = Collections.<String, Object>singletonMap("maxBatchSize", batchMaxSize);
            return ResponseEntity.status(400).body(errorBody(request, "batch_limit_exceeded", msg(request, "batch_limit_exceeded"), details));
        }
        String tenantId = normalize(currentTenant(request));
        @SuppressWarnings("unchecked")
        Map<String, Object> summary = (Map<String, Object>) notificationJobService.batchRetryByIds(tenantId, normalizedJobIds);
        return ResponseEntity.ok(successWithFields(request, "notification_jobs_retried", summary));
    }

    @PostMapping("/jobs/retry-by-filter")
    public ResponseEntity<?> retryByFilter(HttpServletRequest request, @Valid @RequestBody(required = false) V1NotificationRetryByFilterRequest payload) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        String tenantId = normalize(currentTenant(request));
        String status = normalizeStatus(payload == null ? null : payload.getStatus(), "FAILED");
        int page = payload == null || payload.getPage() == null ? 1 : payload.getPage();
        int size = payload == null || payload.getSize() == null ? 20 : payload.getSize();
        if (page < 1 || size < 1) {
            return ResponseEntity.badRequest().body(errorBody(request, "bad_request", msg(request, "bad_request"), null));
        }
        if (size > batchMaxSize) {
            Map<String, Object> details = Collections.<String, Object>singletonMap("maxBatchSize", batchMaxSize);
            return ResponseEntity.status(400).body(errorBody(request, "batch_limit_exceeded", msg(request, "batch_limit_exceeded"), details));
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> summary = (Map<String, Object>) notificationJobService.retryByFilter(tenantId, status, page, size);
        return ResponseEntity.ok(successWithFields(request, "notification_jobs_retried", summary));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeStatus(String value, String fallback) {
        String normalized = normalize(value);
        if (normalized.isEmpty()) {
            normalized = normalize(fallback);
        }
        return normalized.toUpperCase(Locale.ROOT);
    }

    private List<String> normalizeJobIds(List<String> rawJobIds) {
        if (rawJobIds == null || rawJobIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> normalized = new ArrayList<String>(rawJobIds.size());
        for (String rawJobId : rawJobIds) {
            String jobId = normalize(rawJobId);
            if (!jobId.isEmpty()) {
                normalized.add(jobId);
            }
        }
        return normalized;
    }
}

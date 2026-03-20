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
import java.util.Collections;
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
        String tenantId = currentTenant(request);
        @SuppressWarnings("unchecked")
        Map<String, Object> pageBody = (Map<String, Object>) notificationJobService.listJobsPaged(tenantId, status, page, size);
        return ResponseEntity.ok(successWithFields(request, "notification_jobs_listed", pageBody));
    }

    @PostMapping("/jobs/{jobId}/retry")
    public ResponseEntity<?> retry(HttpServletRequest request, @PathVariable String jobId) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        String tenantId = currentTenant(request);
        NotificationJob job = notificationJobService.retry(tenantId, jobId);
        if (job == null) {
            return ResponseEntity.status(404).body(errorBody(request, "notification_job_not_found", msg(request, "notification_job_not_found"), null));
        }
        return ResponseEntity.ok(successWithFields(request, "notification_job_retried", Collections.<String, Object>singletonMap("jobId", job.getId())));
    }

    @PostMapping("/jobs/batch-retry")
    public ResponseEntity<?> batchRetry(HttpServletRequest request, @RequestBody(required = false) V1NotificationBatchRetryRequest payload) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        int requested = payload == null || payload.getJobIds() == null ? 0 : payload.getJobIds().size();
        if (requested > batchMaxSize) {
            Map<String, Object> details = Collections.<String, Object>singletonMap("maxBatchSize", batchMaxSize);
            return ResponseEntity.status(400).body(errorBody(request, "batch_limit_exceeded", msg(request, "batch_limit_exceeded"), details));
        }
        String tenantId = currentTenant(request);
        @SuppressWarnings("unchecked")
        Map<String, Object> summary = (Map<String, Object>) notificationJobService.batchRetryByIds(tenantId, payload == null ? null : payload.getJobIds());
        return ResponseEntity.ok(successWithFields(request, "notification_jobs_retried", summary));
    }

    @PostMapping("/jobs/retry-by-filter")
    public ResponseEntity<?> retryByFilter(HttpServletRequest request, @RequestBody(required = false) V1NotificationRetryByFilterRequest payload) {
        if (!hasAnyRole(request, "ADMIN", "MANAGER")) {
            return ResponseEntity.status(403).body(errorBody(request, "forbidden", msg(request, "forbidden"), null));
        }
        String tenantId = currentTenant(request);
        String status = payload == null ? "FAILED" : payload.getStatus();
        int page = payload == null || payload.getPage() == null ? 1 : payload.getPage();
        int size = payload == null || payload.getSize() == null ? 20 : payload.getSize();
        if (size > batchMaxSize) {
            Map<String, Object> details = Collections.<String, Object>singletonMap("maxBatchSize", batchMaxSize);
            return ResponseEntity.status(400).body(errorBody(request, "batch_limit_exceeded", msg(request, "batch_limit_exceeded"), details));
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> summary = (Map<String, Object>) notificationJobService.retryByFilter(tenantId, status, page, size);
        return ResponseEntity.ok(successWithFields(request, "notification_jobs_retried", summary));
    }
}

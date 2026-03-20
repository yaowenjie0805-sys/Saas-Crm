package com.yao.crm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yao.crm.entity.NotificationJob;
import com.yao.crm.repository.NotificationJobRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class NotificationJobService {

    private final NotificationJobRepository jobRepository;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;
    private final int maxRetries;
    private final String providers;

    public NotificationJobService(NotificationJobRepository jobRepository,
                                  AuditLogService auditLogService,
                                  ObjectMapper objectMapper,
                                  @Value("${integration.notifications.max-retries:5}") int maxRetries,
                                  @Value("${integration.webhooks.providers:WECOM,DINGTALK}") String providers) {
        this.jobRepository = jobRepository;
        this.auditLogService = auditLogService;
        this.objectMapper = objectMapper;
        this.maxRetries = Math.max(1, maxRetries);
        this.providers = providers == null ? "" : providers;
    }

    @Transactional
    public void enqueueSlaEscalated(String tenantId, String instanceId, String taskId, String approverRole) {
        String[] configuredTargets = providers.split(",");
        List<String> targets = new ArrayList<String>();
        List<String> dedupeKeys = new ArrayList<String>();
        Map<String, String> dedupeByTarget = new LinkedHashMap<String, String>();
        for (String targetRaw : configuredTargets) {
            String target = targetRaw == null ? "" : targetRaw.trim().toUpperCase(Locale.ROOT);
            if (target.isEmpty()) continue;
            String dedupeKey = tenantId + "|" + instanceId + "|" + taskId + "|approval_sla_escalated|" + target;
            targets.add(target);
            dedupeKeys.add(dedupeKey);
            dedupeByTarget.put(target, dedupeKey);
        }
        Set<String> existingDedupeKeys = new HashSet<String>();
        if (!dedupeKeys.isEmpty()) {
            for (NotificationJob existing : jobRepository.findByTenantIdAndDedupeKeyIn(tenantId, dedupeKeys)) {
                if (existing.getDedupeKey() != null) existingDedupeKeys.add(existing.getDedupeKey());
            }
        }
        for (String target : targets) {
            String dedupeKey = dedupeByTarget.get(target);
            if (dedupeKey == null || existingDedupeKeys.contains(dedupeKey)) continue;
            try {
                Map<String, Object> payload = new LinkedHashMap<String, Object>();
                payload.put("event", "approval_sla_escalated");
                payload.put("tenantId", tenantId);
                payload.put("instanceId", instanceId);
                payload.put("taskId", taskId);
                payload.put("approverRole", approverRole);
                NotificationJob job = new NotificationJob();
                job.setId(newId("noj"));
                job.setTenantId(tenantId);
                job.setEventType("approval_sla_escalated");
                job.setTarget(target);
                job.setPayload(objectMapper.writeValueAsString(payload));
                job.setStatus("PENDING");
                job.setRetryCount(0);
                job.setMaxRetries(maxRetries);
                job.setDedupeKey(dedupeKey);
                job.setNextRetryAt(LocalDateTime.now());
                jobRepository.save(job);
                auditLogService.record("system", "SYSTEM", "NOTIFY_ENQUEUED", "NOTIFICATION_JOB", job.getId(), "Queued notification job", tenantId);
            } catch (Exception ex) {
                auditLogService.record("system", "SYSTEM", "NOTIFY_ENQUEUE_FAILED", "NOTIFICATION_JOB", taskId, String.valueOf(ex.getMessage()), tenantId);
            }
        }
    }

    @Transactional
    public int processQueue() {
        List<NotificationJob> jobs = jobRepository.findByStatusInAndNextRetryAtBeforeOrderByCreatedAtAsc(Arrays.asList("PENDING", "RETRY"), LocalDateTime.now());
        int processed = 0;
        for (NotificationJob job : jobs) {
            processed++;
            try {
                job.setStatus("RUNNING");
                jobRepository.save(job);
                boolean ok = dispatch(job);
                if (ok) {
                    job.setStatus("SUCCESS");
                    job.setLastError(null);
                    jobRepository.save(job);
                    auditLogService.record("system", "SYSTEM", "NOTIFY_SUCCESS", job.getTarget(), job.getId(), "notification sent", job.getTenantId());
                } else {
                    failOrRetry(job, "dispatch returned false");
                }
            } catch (Exception ex) {
                failOrRetry(job, ex.getMessage());
            }
        }
        return processed;
    }

    public Map<String, Object> listJobsPaged(String tenantId, String status, int page, int size) {
        int finalPage = Math.max(1, page);
        int finalSize = Math.max(1, Math.min(size, 200));
        Pageable pageable = PageRequest.of(finalPage - 1, finalSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<NotificationJob> rows = (status == null || status.trim().isEmpty() || "ALL".equalsIgnoreCase(status))
                ? jobRepository.findByTenantId(tenantId, pageable)
                : jobRepository.findByTenantIdAndStatus(tenantId, status.trim().toUpperCase(Locale.ROOT), pageable);
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        for (NotificationJob row : rows.getContent()) {
            items.add(toView(row));
        }
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("items", items);
        out.put("page", finalPage);
        out.put("size", finalSize);
        out.put("totalPages", Math.max(1, rows.getTotalPages()));
        out.put("total", rows.getTotalElements());
        return out;
    }

    @Transactional
    public NotificationJob retry(String tenantId, String jobId) {
        Optional<NotificationJob> optional = jobRepository.findByIdAndTenantId(jobId, tenantId);
        if (!optional.isPresent()) return null;
        NotificationJob job = optional.get();
        if (!"FAILED".equalsIgnoreCase(job.getStatus())) return job;
        moveToRetry(job);
        return jobRepository.save(job);
    }

    @Transactional
    public Map<String, Object> batchRetryByIds(String tenantId, List<String> jobIds) {
        int requested = jobIds == null ? 0 : jobIds.size();
        int succeeded = 0;
        int skipped = 0;
        int notFound = 0;
        int forbidden = 0;
        if (jobIds != null) {
            for (String rawId : jobIds) {
                String id = rawId == null ? "" : rawId.trim();
                if (id.isEmpty()) { skipped++; continue; }
                Optional<NotificationJob> optional = jobRepository.findById(id);
                if (!optional.isPresent()) {
                    notFound++;
                    continue;
                }
                NotificationJob job = optional.get();
                if (!tenantId.equals(job.getTenantId())) {
                    forbidden++;
                    continue;
                }
                if (!"FAILED".equalsIgnoreCase(job.getStatus())) {
                    skipped++;
                    continue;
                }
                moveToRetry(job);
                jobRepository.save(job);
                succeeded++;
            }
        }
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("requested", requested);
        out.put("succeeded", succeeded);
        out.put("skipped", skipped);
        out.put("notFound", notFound);
        out.put("forbidden", forbidden);
        auditLogService.record("system", "SYSTEM", "NOTIFY_BATCH_RETRY", "NOTIFICATION_JOB", tenantId, out.toString(), tenantId);
        return out;
    }

    @Transactional
    public Map<String, Object> retryByFilter(String tenantId, String status, int page, int size) {
        int finalPage = Math.max(1, page);
        int finalSize = Math.max(1, Math.min(size, 200));
        // Force FAILED regardless of input status.
        Pageable pageable = PageRequest.of(finalPage - 1, finalSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<NotificationJob> rows = jobRepository.findByTenantIdAndStatus(tenantId, "FAILED", pageable);
        int requested = rows.getContent().size();
        int succeeded = 0;
        int skipped = 0;
        for (NotificationJob job : rows.getContent()) {
            if (!"FAILED".equalsIgnoreCase(job.getStatus())) {
                skipped++;
                continue;
            }
            moveToRetry(job);
            jobRepository.save(job);
            succeeded++;
        }
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("requested", requested);
        out.put("succeeded", succeeded);
        out.put("skipped", skipped);
        out.put("status", status == null ? "" : status);
        out.put("page", finalPage);
        out.put("size", finalSize);
        auditLogService.record("system", "SYSTEM", "NOTIFY_RETRY_BY_FILTER", "NOTIFICATION_JOB", tenantId, out.toString(), tenantId);
        return out;
    }

    private void failOrRetry(NotificationJob job, String reason) {
        int nextRetryCount = (job.getRetryCount() == null ? 0 : job.getRetryCount()) + 1;
        job.setRetryCount(nextRetryCount);
        job.setLastError(reason == null ? "unknown" : reason);
        if (nextRetryCount >= (job.getMaxRetries() == null ? maxRetries : job.getMaxRetries())) {
            job.setStatus("FAILED");
            job.setNextRetryAt(null);
            auditLogService.record("system", "SYSTEM", "NOTIFY_FAILED", job.getTarget(), job.getId(), job.getLastError(), job.getTenantId());
        } else {
            long delaySeconds = (long) Math.min(300, Math.pow(2, nextRetryCount) * 5);
            job.setStatus("RETRY");
            job.setNextRetryAt(LocalDateTime.now().plusSeconds(delaySeconds));
            auditLogService.record("system", "SYSTEM", "NOTIFY_RETRY", job.getTarget(), job.getId(), "retry in " + delaySeconds + "s", job.getTenantId());
        }
        jobRepository.save(job);
    }

    private boolean dispatch(NotificationJob job) {
        // This stage keeps delivery internal and auditable; actual external HTTP can be plugged here.
        String payload = job.getPayload() == null ? "{}" : job.getPayload();
        auditLogService.record("system", "SYSTEM", "WEBHOOK_DISPATCH", job.getTarget(), job.getId(), payload, job.getTenantId());
        return true;
    }

    private void moveToRetry(NotificationJob job) {
        job.setStatus("RETRY");
        job.setNextRetryAt(LocalDateTime.now());
        job.setLastError(null);
    }

    private Map<String, Object> toView(NotificationJob row) {
        Map<String, Object> item = new LinkedHashMap<String, Object>();
        item.put("jobId", row.getId());
        item.put("tenantId", row.getTenantId());
        item.put("eventType", row.getEventType());
        item.put("target", row.getTarget());
        item.put("status", row.getStatus());
        item.put("retryCount", row.getRetryCount());
        item.put("maxRetries", row.getMaxRetries());
        item.put("nextRetryAt", row.getNextRetryAt());
        item.put("lastError", row.getLastError());
        item.put("createdAt", row.getCreatedAt());
        item.put("updatedAt", row.getUpdatedAt());
        return item;
    }

    private String newId(String prefix) {
        return prefix + "_" + Long.toString(System.currentTimeMillis(), 36) + String.format("%03d", (int) (Math.random() * 1000));
    }
}
